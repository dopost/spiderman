package com.panda.http.spiderman;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by steven on 2016/10/25.
 */

public class DownloadManager {

    private Map<String, DownLoadTask> taskList = new ConcurrentHashMap<String, DownLoadTask>();

    private DownloadManager(){
    }

    private static class SingletonHolder {
        private static DownloadManager instance = new DownloadManager();
    }

    public static DownloadManager newInstance(){
        return SingletonHolder.instance;
    }

    public void addTask(String url, DownLoadTask task) {
        if (task == null || url == null) {
            return;
        }
        if (!taskList.containsKey(url)) {
            task.setUrl(url);
            taskList.put(url, task);
            task.start();
        }
    }

    public void removeTask(String url) {
        if (taskList.containsKey(url)) {
           taskList.remove(url);
        }
    }

    public DownLoadTask getTask(String id) {
        return taskList.get(id);
    }



}
