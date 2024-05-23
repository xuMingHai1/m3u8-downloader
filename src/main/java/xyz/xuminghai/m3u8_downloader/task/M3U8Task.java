package xyz.xuminghai.m3u8_downloader.task;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.javacpp.Loader;
import xyz.xuminghai.m3u8_downloader.http.M3U8HttpClient;
import xyz.xuminghai.m3u8_downloader.m3u8.M3U8Key;
import xyz.xuminghai.m3u8_downloader.m3u8.M3U8Parse;
import xyz.xuminghai.m3u8_downloader.m3u8.MediaPlay;
import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;
import xyz.xuminghai.m3u8_downloader.util.DurationUtils;
import xyz.xuminghai.m3u8_downloader.util.FileSizeUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2024/4/23 下午7:15 星期二<br/>
 * m3u8任务
 *
 * @author xuMingHai
 */
@Slf4j
public class M3U8Task extends Task<Void> implements AutoCloseable {

    private static final Pattern SIZE_PATTERN = Pattern.compile("^size=\\s*(\\d+kB)");

    private final M3U8 m3u8;

    private final M3U8HttpClient m3u8HttpClient;

    private final long taskMaxWaitTime;
    private final long startTime = System.currentTimeMillis();

    private final AtomicBoolean pauseState = new AtomicBoolean();
    private final AtomicBoolean disablePause = new AtomicBoolean();
    private final AtomicReference<Thread> executorThread = new AtomicReference<>();
    private final AtomicReference<Throwable> retryableException = new AtomicReference<>();

    private final BooleanProperty disablePauseProperty = new SimpleBooleanProperty();
    private final ObjectProperty<Throwable> retryableExceptionProperty = new SimpleObjectProperty<>();


    private final double totalWork = 1.0;
    private double workDone = 0.0;

    public M3U8Task(M3U8 m3u8) {
        // 更新进度 0%
        super.updateProgress(workDone, totalWork);
        this.m3u8 = m3u8;
        this.taskMaxWaitTime = m3u8.timeout().toMillis();
        this.m3u8HttpClient = new M3U8HttpClient(m3u8.timeout(), m3u8.downloadTempDirPath(), m3u8.proxySelector());
        log.info("M3U8任务创建完成，{}", m3u8);
    }


    @Override
    protected Void call() throws Exception {
        executorThread.set(Thread.currentThread());
        Files.createDirectory(m3u8.downloadTempDirPath());
        log.info("下载临时目录创建成功，downloadTempPath = {}", m3u8.downloadTempDirPath());
        // 更新进度 2%
        super.updateProgress(workDone = 0.02, totalWork);

        // 下载m3u8文件
        log.info("开始下载m3u8文件");
        Path m3u8FilePath;
        for (; ; ) {
            super.updateMessage("正在下载M3U8文件");
            try {
                m3u8FilePath = m3u8HttpClient.downloadM3U8(m3u8.m3u8Uri());
            }
            catch (InterruptedException e) {
                // 不是暂停状态
                if (isNotPauseState()) {
                    return null;
                }
                super.updateMessage("暂停下载M3U8文件");
                log.info("暂停m3u8文件下载");
                // 暂停等待恢复
                if (awaitResume()) {
                    log.info("恢复m3u8文件下载");
                    continue;
                }
                return null;
            }
            // 可重试异常
            catch (IOException e) {
                log.error("m3u8文件下载异常", e);
                super.updateMessage("m3u8文件下载异常");
                // 可重试失败
                retryableFailure(e);
                // 暂停等待恢复
                if (awaitResume()) {
                    log.info("重试m3u8文件下载");
                    continue;
                }
                return null;
            }
            log.info("m3u8下载任务成功");
            // 更新进度 6%
            super.updateProgress(workDone = 0.06, totalWork);
            break;
        }

        // 解析m3u8文件
        log.info("开始解析m3u8文件，m3u8FilePath = {}", m3u8FilePath);
        super.updateMessage("正在解析M3U8文件");
        List<MediaPlay> playList = M3U8Parse.parse(m3u8.m3u8Uri(), m3u8FilePath);
        log.atInfo().setMessage("m3u8文件解析成功，playList.size = {}")
                .addArgument(playList::size)
                .log();
        // 更新进度 8%
        super.updateProgress(workDone = 0.08, totalWork);

        // 是否需要下载密钥
        for (int i = 0; i < playList.size(); i++) {
            final M3U8Key m3u8Key = playList.get(i).key();
            switch (m3u8Key.getMethod()) {
                case NONE -> {
                    // 无加密数据
                }
                case AES_128 -> {
                    // key是唯一可变的
                    if (m3u8Key.getKey() == null) {
                        log.info("AES-128加密方法，下载密钥");
                        super.updateMessage("AES-128加密方法，正在下载密钥");
                        final byte[] bytes;
                        try {
                            bytes = m3u8HttpClient.downloadKey(m3u8Key.getUri());
                        }
                        catch (InterruptedException e) {
                            // 不是暂停状态
                            if (isNotPauseState()) {
                                return null;
                            }
                            log.info("暂停aes-128密钥下载");
                            super.updateMessage("暂停aes-128密钥下载");
                            // 暂停等待恢复
                            if (awaitResume()) {
                                log.info("恢复aes-128密钥下载");
                                i--;
                                continue;
                            }
                            return null;
                        }
                        catch (IOException e) {
                            log.error("aes-128密钥下载异常", e);
                            super.updateMessage("aes-128密钥下载异常");
                            // 可重试失败
                            retryableFailure(e);
                            // 暂停等待恢复
                            if (awaitResume()) {
                                log.info("重试aes-128密钥下载");
                                i--;
                                continue;
                            }
                            return null;
                        }
                        log.info("AES-128密钥下载成功");
                        m3u8Key.setKey(bytes);
                    }
                }
                case SAMPLE_AES -> throw new IllegalStateException("不支持的加密");
            }
        }
        // 更新进度 10%
        super.updateProgress(workDone = 0.1, totalWork);

        // ts 文件下载 75%
        List<TsFile> tsFileList = new ArrayList<>(playList.size());
        final double tsProgressUnit = 0.75 / playList.size();
        // 翻转集合，方便后序查找删除
        playList = playList.reversed();
        for (; ; ) {
            try {
                // 创建ts文件列表下载任务
                final List<Future<TsFile>> tsTaskList = m3u8HttpClient.downloadTs(playList);
                log.atInfo().setMessage("创建ts文件列表下载任务成功，tsTaskList.size = {}")
                        .addArgument(tsTaskList::size)
                        .log();
                super.updateMessage("ts文件列表下载任务数量 = " + tsTaskList.size());
                // 等待ts任务完成
                final List<TsFile> doneTask = tsTask(tsTaskList, tsProgressUnit);
                tsFileList.addAll(doneTask);
            }
            catch (TsTaskInterruptedException e) {
                if (isNotPauseState()) {
                    return null;
                }
                log.info("暂停ts文件下载");
                super.updateMessage("暂停ts文件下载");
                // 暂停等待恢复
                if (awaitResume()) {
                    log.info("恢复ts文件下载");
                    final List<TsFile> doneTask = e.getDoneTask();
                    for (TsFile tsFile : doneTask) {
                        tsFileList.add(tsFile);
                        playList.removeIf(mediaPlay -> mediaPlay.sequence() == tsFile.sequence());
                    }
                    continue;
                }
                return null;
            }
            catch (TsExecutionException e) {
                super.updateMessage("ts文件下载异常");
                // 可重试失败，获取包装的异常
                retryableFailure(e.getCause());
                // 暂停等待恢复
                if (awaitResume()) {
                    log.info("重试ts文件下载");
                    final List<TsFile> doneTask = e.getDoneTask();
                    for (TsFile tsFile : doneTask) {
                        tsFileList.add(tsFile);
                        playList.removeIf(mediaPlay -> mediaPlay.sequence() == tsFile.sequence());
                    }
                    continue;
                }
                return null;
            }
            break;
        }
        tsFileList.sort(Comparator.comparingInt(TsFile::sequence));

        // 执行ffmpeg合并ts
        log.info("执行合并ts文件列表任务");
        super.updateMessage("正在合并ts文件");
        // 禁用暂停
        disabledPause();
        ffmpegTask(tsFileList);

        final String successMessage = "%s下载成功，视频大小：%s，耗时：%s".formatted(m3u8.filePath().getFileName(),
                FileSizeUtils.convertString(Files.size(m3u8.filePath())),
                DurationUtils.chineseString(Duration.ofMillis(System.currentTimeMillis() - startTime)));
        log.info(successMessage);
        super.updateMessage(successMessage);
        // 更新进度 100%
        super.updateProgress(totalWork, totalWork);
        return null;
    }

    private List<TsFile> tsTask(List<Future<TsFile>> tsTaskList, double tsProgressUnit) throws ExecutionException, InterruptedException {
        log.info("等待ts文件下载任务完成");
        final List<Future<TsFile>> undoneTask = new LinkedList<>(tsTaskList);
        final List<TsFile> doneTask = new LinkedList<>();

        // 循环等待每个任务的完成情况
        while (!undoneTask.isEmpty()) {
            final Iterator<Future<TsFile>> iterator = undoneTask.iterator();
            while (iterator.hasNext()) {
                // 计算等待时长，size越大等待时间越短，反之越小等待时间越大
                final double d = 1.0 / undoneTask.size();
                long min = 1L;
                final long timeout = min + (long) ((taskMaxWaitTime - min) * d);
                try {
                    final TsFile tsFile = iterator.next().get(timeout, TimeUnit.MILLISECONDS);
                    doneTask.add(tsFile);
                    // 任务完成后从未完成任务列表删除
                    iterator.remove();
                    // 更新进度
                    super.updateMessage("ts文件下载任务未完成数量 = " + undoneTask.size());
                    super.updateProgress(workDone += tsProgressUnit, totalWork);
                }
                catch (InterruptedException e) {
                    // 当前线程中断，取消未完成任务
                    log.atWarn().setMessage("暂停ts文件下载，取消所有未完成任务，undoneTask.Size = {}")
                            .addArgument(undoneTask::size)
                            .log();
                    undoneTask.forEach(future -> future.cancel(true));
                    throw new TsTaskInterruptedException(doneTask);
                }
                catch (ExecutionException e) {
                    // 任务执行异常，取消未完成任务
                    iterator.remove();
                    log.atError().setMessage("ts下载出现异常，取消所有未完成任务，undoneTask.Size = {}")
                            .addArgument(undoneTask::size)
                            .setCause(e)
                            .log();
                    undoneTask.forEach(future -> future.cancel(true));
                    throw new TsExecutionException(doneTask, e.getCause());
                }
                catch (TimeoutException _) {
                    // 等待超时，获取下一个任务
                }
            }
            log.atInfo().setMessage("undoneTask.size = {}")
                    .addArgument(undoneTask::size)
                    .log();
        }
        log.info("所有ts文件下载任务已经完成");
        return doneTask;
    }


    private void ffmpegTask(List<TsFile> tsFileList) throws IOException, InterruptedException {
        // 生成数据文件
        final Path playListPath = m3u8.downloadTempDirPath().resolve("playList.txt");
        final StringBuilder stringBuilder = new StringBuilder();
        tsFileList.forEach(tsFile -> stringBuilder.append("file ")
                .append('\'')
                .append(tsFile.path())
                .append('\'')
                .append(System.lineSeparator()));
        Files.writeString(playListPath, stringBuilder);
        log.debug("生成输入数据文件，input = {}", playListPath);

        log.info("MP4文件路径 = {}", m3u8.filePath());
        final String ffmpeg = Loader.load(ffmpeg.class);
        final ProcessBuilder processBuilder = new ProcessBuilder(ffmpeg,
                "-hide_banner",
                "-f", "concat",
                "-safe", "0",
                "-i", playListPath.toString(),
                "-c", "copy",
                m3u8.filePath().toString());
        log.debug("command = {}", processBuilder.command());

        // 将ffmpeg命令写入文件
        final Path ffmpegCommandPath = m3u8.downloadTempDirPath().resolve("ffmpeg_command.log");
        final StringJoiner stringJoiner = new StringJoiner(" ");
        processBuilder.command().forEach(stringJoiner::add);
        Files.writeString(ffmpegCommandPath, stringJoiner.toString());

        // 启动ffmpeg进程
        final Process process = processBuilder.start();
        // 计算所有ts文件大小
        double progress = 0.15;
        double totalSize = 0;
        for (TsFile tsFile : tsFileList) {
            totalSize += Files.size(tsFile.path());
        }
        final Path ffmpegOutput = m3u8.downloadTempDirPath().resolve("ffmpeg_output.log");
        // 将输出写入文件和解析
        try (final BufferedReader bufferedReader = process.errorReader(StandardCharsets.UTF_8);
             final BufferedWriter bufferedWriter = Files.newBufferedWriter(ffmpegOutput)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
                // 匹配当前size
                final Matcher matcher = SIZE_PATTERN.matcher(line);
                if (matcher.find()) {
                    // 粗略计算进度
                    final long fileSize = FileSizeUtils.parseString(matcher.group(1));
                    final double work = fileSize / totalSize * progress;
                    super.updateProgress(workDone += work, totalWork);
                    totalSize += fileSize;
                    progress -= work;
                }
            }
        }
        process.waitFor();
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        close();
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        log.info("取消M3U8任务，{}", m3u8);
        close();
    }

    @Override
    public void close() {
        m3u8HttpClient.close();
        try {
            deleteDirectory();
        }
        catch (IOException _) {
            // 删除失败时忽略
        }
    }

    private void deleteDirectory() throws IOException {
        log.debug("删除临时目录文件");
        Files.walkFileTree(m3u8.downloadTempDirPath(), new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isNotPauseState() {
        return !pauseState.get();
    }

    private boolean isNotDisabledPause() {
        return !disablePause.get();
    }

    boolean pause() {
        // 中断当前线程
        if (isNotDisabledPause() &&
                pauseState.compareAndSet(false, true)) {
            executorThread.get().interrupt();
            return true;
        }
        return false;
    }

    private final CyclicBarrier cyclicBarrier = new CyclicBarrier(Integer.MAX_VALUE);

    boolean resume() {
        if (isNotDisabledPause() &&
                pauseState.compareAndSet(true, false)) {
            m3u8HttpClient.reset();
            cyclicBarrier.reset();
            return true;
        }
        return false;
    }

    private boolean awaitResume() {
        try {
            cyclicBarrier.await();
        }
        catch (BrokenBarrierException _) {

        }
        catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    ReadOnlyBooleanProperty disablePauseProperty() {
        return disablePauseProperty;
    }

    private void disabledPause() {
        // 设置禁用暂停
        disablePause.set(true);
        // 禁用暂停
        Platform.runLater(() -> disablePauseProperty.set(true));
        // 避免之前设置的中断状态
        //noinspection ResultOfMethodCallIgnored
        Thread.interrupted();
    }

    ReadOnlyObjectProperty<Throwable> retryableExceptionProperty() {
        return retryableExceptionProperty;
    }

    private void retryableFailure(Throwable e) {
        retryableException.set(e);
        Platform.runLater(() -> retryableExceptionProperty.set(e));
    }

    boolean retryable() {
        if (retryableException.get() != null) {
            cyclicBarrier.reset();
            return true;
        }
        return false;
    }
}
