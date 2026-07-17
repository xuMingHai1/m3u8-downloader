/*
 * SPDX-FileCopyrightText: 2024 xuMingHai <173535609@qq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package xyz.xuminghai.m3u8_downloader.task;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.ffmpeg;
import org.bytedeco.javacpp.Loader;
import xyz.xuminghai.m3u8_downloader.config.CommonData;
import xyz.xuminghai.m3u8_downloader.http.M3U8HttpClient;
import xyz.xuminghai.m3u8_downloader.m3u8.M3U8Key;
import xyz.xuminghai.m3u8_downloader.m3u8.M3U8Parse;
import xyz.xuminghai.m3u8_downloader.m3u8.MediaPlay;
import xyz.xuminghai.m3u8_downloader.m3u8.TsFile;
import xyz.xuminghai.m3u8_downloader.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
public class M3U8Task extends Task<Path> {

    private static final Pattern SIZE_PATTERN = Pattern.compile("^size=\\s*(\\d+kB) ");

    private final AtomicReference<M3U8> lastM3U8 = new AtomicReference<>(),
            currentM3U8 = new AtomicReference<>();

    private final AtomicReference<M3U8HttpClient> m3u8HttpClient = new AtomicReference<>();

    private List<TsFile> tsFileList;

    private final long startTime = System.currentTimeMillis();


    private final double totalWork = 1.0;
    private double workDone = 0.0;

    public M3U8Task(M3U8 m3u8) {
        // 更新进度 0%
        super.updateProgress(workDone, totalWork);
        lastM3U8.setOpaque(m3u8);
        currentM3U8.setOpaque(m3u8);
        m3u8HttpClient.setOpaque(new M3U8HttpClient(m3u8.timeout(), m3u8.downloadTempDirPath(), m3u8.proxySelector()));
        log.info("M3U8任务创建完成，m3u8Uri = {}, filePath = {}, downloadTempDirPath = {}, timeout = {}",
                LogMaskUtils.uri(m3u8.m3u8Uri()), m3u8.filePath(), m3u8.downloadTempDirPath(), m3u8.timeout());
    }

    private M3U8 getM3U8() {
        return currentM3U8.getOpaque();
    }

    private M3U8HttpClient getM3U8HttpClient() {
        return m3u8HttpClient.getOpaque();
    }

    @Override
    protected Path call() throws Exception {
        executorThread.set(Thread.currentThread());
        Files.createDirectory(getM3U8().downloadTempDirPath());
        log.info("下载临时目录创建成功，downloadTempPath = {}", getM3U8().downloadTempDirPath());
        // 下载速度统计
        downloadSpeedStatistics();
        // 更新进度 2%
        super.updateProgress(workDone = 0.02, totalWork);

        // 下载m3u8文件
        log.info("开始下载m3u8文件");
        Path m3u8FilePath;
        for (; ; ) {
            super.updateMessage("正在下载M3U8文件");
            try {
                m3u8FilePath = getM3U8HttpClient().downloadM3U8(getM3U8().m3u8Uri());
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
        List<MediaPlay> playList = M3U8Parse.parse(getM3U8().m3u8Uri(), m3u8FilePath);
        log.atInfo().setMessage("m3u8文件解析成功，playList.size = {}")
                .addArgument(playList::size)
                .log();
        // 更新进度 8%
        super.updateProgress(workDone = 0.08, totalWork);

        // 是否需要下载密钥
        final Map<URI, byte[]> keyCache = new HashMap<>();
        for (int i = 0; i < playList.size(); i++) {
            final M3U8Key m3u8Key = playList.get(i).key();
            switch (m3u8Key.getMethod()) {
                case NONE -> {
                    // 无加密数据
                }
                case AES_128 -> {
                    // key是唯一可变的
                    if (m3u8Key.getKey() == null) {
                        byte[] bytes = keyCache.get(m3u8Key.getUri());
                        if (bytes == null) {
                            log.info("AES-128加密方法，下载密钥");
                            super.updateMessage("AES-128加密方法，正在下载密钥");
                            try {
                                bytes = getM3U8HttpClient().downloadKey(m3u8Key.getUri());
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
                            keyCache.put(m3u8Key.getUri(), bytes);
                            log.info("AES-128密钥下载成功");
                        }
                        m3u8Key.setKey(bytes);
                    }
                }
                case SAMPLE_AES -> throw new IllegalStateException("不支持的加密");
            }
        }
        // 更新进度 10%
        super.updateProgress(workDone = 0.1, totalWork);

        // ts 文件下载 75%
        tsFileList = new ArrayList<>(playList.size());
        final double tsProgressUnit = 0.75 / playList.size();
        for (; ; ) {
            try {
                // 创建ts文件列表下载任务
                final List<Future<TsFile>> tsTaskList = getM3U8HttpClient().downloadTs(playList);
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
                    tsTaskResume(e, playList);
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
                    tsTaskResume(e, playList);
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
        // 关闭下载速率统计
        closeDownloadSpeedStatistics();
        // 执行ffmpeg合并任务
        ffmpegTask(tsFileList);

        final String successMessage = "%s下载成功，视频大小：%s，耗时：%s".formatted(getM3U8().filePath().getFileName(),
                FileSizeUtils.convertString(Files.size(getM3U8().filePath())),
                DurationUtils.chineseString(Duration.ofMillis(System.currentTimeMillis() - startTime)));
        log.info(successMessage);
        super.updateMessage(successMessage);

        // 更新进度 100%
        super.updateProgress(totalWork, totalWork);
        return getM3U8().filePath();
    }

    private void tsTaskResume(TsTaskException tsTaskException, List<MediaPlay> playList) {
        final List<TsFile> doneTask = tsTaskException.getDoneTask();
        for (TsFile tsFile : doneTask) {
            tsFileList.add(tsFile);
            playList.removeIf(mediaPlay -> mediaPlay.sequence() == tsFile.sequence());
        }
        // 是否更改了临时下载目录
        if (tempDirPathVary()) {
            final Path downloadTempDirPath = getM3U8().downloadTempDirPath();
            // 修改已完成的ts文件列表路径
            final List<TsFile> newTsFileList = new ArrayList<>(tsFileList.size());
            tsFileList.forEach(tsFile -> newTsFileList.add(new TsFile(tsFile.sequence(),
                            downloadTempDirPath.resolve(tsFile.path().getFileName()))
                    )
            );
            tsFileList = newTsFileList;
        }
    }


    private List<TsFile> tsTask(List<Future<TsFile>> tsTaskList, double tsProgressUnit) throws ExecutionException, InterruptedException {
        log.info("等待ts文件下载任务完成");
        final List<Future<TsFile>> undoneTask = new LinkedList<>(tsTaskList);
        final List<TsFile> doneTask = new LinkedList<>();

        // 循环等待每个任务的完成情况
        while (!undoneTask.isEmpty()) {
            final Iterator<Future<TsFile>> iterator = undoneTask.iterator();
            while (iterator.hasNext()) {
                try {
                    final TsFile tsFile = iterator.next().get();
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
        final Path playListPath = getM3U8().downloadTempDirPath().resolve("playList.txt");
        final StringBuilder stringBuilder = new StringBuilder();
        tsFileList.forEach(tsFile -> stringBuilder.append("file ")
                .append('\'')
                .append(tsFile.path())
                .append('\'')
                .append(System.lineSeparator()));
        Files.writeString(playListPath, stringBuilder);
        log.debug("生成输入数据文件，input = {}", playListPath);

        log.info("MP4文件路径 = {}", getM3U8().filePath());
        final String ffmpeg = Loader.load(ffmpeg.class);
        final ProcessBuilder processBuilder = new ProcessBuilder(ffmpeg,
                "-hide_banner",
                "-f", "concat",
                "-safe", "0",
                "-i", playListPath.toString(),
                "-c", "copy",
                getM3U8().filePath().toString());
        log.debug("command = {}", processBuilder.command());

        // 将ffmpeg命令写入文件
        final Path ffmpegCommandPath = getM3U8().downloadTempDirPath().resolve("ffmpeg_command.log");
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
        final Path ffmpegOutput = getM3U8().downloadTempDirPath().resolve("ffmpeg_output.log");
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
                    super.updateMessage("合并ts文件任务：".concat(line));
                }
            }
        }
        process.waitFor();
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        log.info("取消M3U8任务，{}", getM3U8());
    }

    @Override
    protected void done() {
        super.done();
        // 正常完成时删除临时目录文件
        if (super.state() == Future.State.SUCCESS) {
            log.debug("删除临时目录文件");
            try {
                DirectoryUtils.deleteDirectory(getM3U8().downloadTempDirPath());
            }
            catch (IOException _) {
                // 删除失败时忽略
            }
        }
        closeDownloadSpeedStatistics();
        getM3U8HttpClient().close();
    }

    private void updateCurrentM3U8(M3U8 m3u8) {
        lastM3U8.set(currentM3U8.get());
        currentM3U8.set(m3u8);
    }

    /*
        暂停和恢复
     */
    private final AtomicBoolean pauseState = new AtomicBoolean();
    private final AtomicReference<Thread> executorThread = new AtomicReference<>();

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
            closeDownloadSpeedStatistics();
            return true;
        }
        return false;
    }

    private final CyclicBarrier cyclicBarrier = new CyclicBarrier(Integer.MAX_VALUE);

    boolean resume(M3U8 m3u8) {
        if (isNotDisabledPause() &&
                pauseState.compareAndSet(true, false)) {
            getM3U8HttpClient().reset();
            updateCurrentM3U8(m3u8);
            cyclicBarrier.reset();
            downloadSpeedStatistics();
            return true;
        }
        return false;
    }

    private boolean awaitResume() throws IOException {
        try {
            cyclicBarrier.await();
        }
        catch (BrokenBarrierException _) {

        }
        catch (InterruptedException e) {
            return false;
        }
        resumeTask();
        return true;
    }

    private boolean tempDirPathVary() {
        return !lastM3U8.getOpaque().downloadTempDirPath()
                .equals(currentM3U8.getOpaque().downloadTempDirPath());
    }

    private void resumeTask() throws IOException {
        final M3U8 last = lastM3U8.getOpaque();
        final M3U8 current = currentM3U8.getOpaque();

        // 参数没有变化
        if (last.equals(current)) {
            return;
        }

        // 下载的临时目录
        if (tempDirPathVary()) {
            log.info("修改下载临时目录，src = {} dst= {}",
                    last.downloadTempDirPath(), current.downloadTempDirPath());
            super.updateMessage("修改下载临时目录");
            Files.move(last.downloadTempDirPath(), current.downloadTempDirPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        getM3U8HttpClient().close();
        m3u8HttpClient.setOpaque(new M3U8HttpClient(
                current.timeout(), current.downloadTempDirPath(), current.proxySelector())
        );
    }


    /*
        禁用暂停
     */
    private final BooleanProperty disablePauseProperty = new SimpleBooleanProperty();
    private final AtomicBoolean disablePause = new AtomicBoolean();

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


    /*
        异常重试
     */
    private final ObjectProperty<Throwable> retryableExceptionProperty = new SimpleObjectProperty<>();
    private final AtomicReference<Throwable> retryableException = new AtomicReference<>();

    ReadOnlyObjectProperty<Throwable> retryableExceptionProperty() {
        return retryableExceptionProperty;
    }

    private void retryableFailure(Throwable e) {
        retryableException.set(e);
        closeDownloadSpeedStatistics();
        Platform.runLater(() -> retryableExceptionProperty.set(e));
    }

    boolean retry(M3U8 m3u8) {
        if (retryableException.getAndSet(null) != null) {
            getM3U8HttpClient().reset();
            updateCurrentM3U8(m3u8);
            cyclicBarrier.reset();
            downloadSpeedStatistics();
            return true;
        }
        return false;
    }


    /*
        下载速率
     */
    private final AtomicReference<Thread> downloadSpeedStatistics = new AtomicReference<>();
    private final StringProperty downloadSpeedProperty = new SimpleStringProperty();

    ReadOnlyStringProperty downloadSpeedProperty() {
        return downloadSpeedProperty;
    }

    private void updateDownloadSpeed(String downloadSpeed) {
        Platform.runLater(() -> downloadSpeedProperty.set(downloadSpeed));
    }

    private void closeDownloadSpeedStatistics() {
        Optional.ofNullable(downloadSpeedStatistics.getAndSet(null))
                .ifPresent(Thread::interrupt);
    }

    private void downloadSpeedStatistics() {
        CommonData.EXECUTOR.execute(new Runnable() {
            private long lastDirectorySize;

            @Override
            public void run() {
                log.debug("开始下载速度统计");
                downloadSpeedStatistics.set(Thread.currentThread());
                for (; ; ) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(1000L);
                    }
                    catch (InterruptedException e) {
                        log.debug("下载速度统计关闭");
                        updateDownloadSpeed("");
                        break;
                    }

                    final long currentDownloadByte = getM3U8HttpClient().currentDownloadByte();
                    final String downloadSpeed = BitstreamUtils.byteSizeConvertBitstreamString(currentDownloadByte - lastDirectorySize);
                    lastDirectorySize = currentDownloadByte;
                    log.debug("下载速率 = {}ps", downloadSpeed);
                    updateDownloadSpeed(downloadSpeed.concat("ps"));
                }
            }
        });
    }
}
