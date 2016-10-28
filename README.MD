# Android网络请求库

基于retrofit2+okhttp3封装的网络库

## 特性
* 加入基础API
* 支持离线缓存
* 支持https
* 支持Cookie
* 支持文件上传和下载
* 支持统一Header

## 使用
	final SpiderMan spiderMan = new SpiderMan.Builder(this)
                .baseUrl("http://172.18.8.75:5000/")
                .openLog()  //打开网络日志
                .addHttps(hosts,certificates) //添加https支持
                .addDefaultCallbackExecutor()  //添加回调在非UI线程执行,主要用于下载
                .build();
                
  
### 同步Get
	Map<String, String> parameters = new HashMap<>();
                parameters.put("start", "0");
                parameters.put("count", "1");
                Music result = spiderMan.get("music/250", parameters, Music.class);
  
### 同步Post
	Map<String, String> parameters = new HashMap<>();
                parameters.put("username", "zx");
                parameters.put("password", "1234");
                User result = spiderMan.post("user/create", parameters, User.class);
                  
### 异步Get
	Map<String, String> parameters = new HashMap<>();
                parameters.put("start", "0");
                parameters.put("count", "1");
	spiderMan.callGet("music/250", parameters,
                new SpiderMan.ResponseCallBack<Music>() {

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onSuccee(Music response) {
                        System.out.println(response);
                    }
                });
  
### 异步POST
	Map<String, String> parameters = new HashMap<>();
                parameters.put("username", "zx");
                parameters.put("password", "1234");
                
	spiderMan.callPost("user/create", parameters,
                new SpiderMan.ResponseCallBack<User>() {

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onSuccee(User response) {
                        System.out.println(response);
                    }
                });
                
  
### 下载
	String downUrl = "http://www.dopost.com/game/cloud/aaa.apk";
	spiderMan.download(downUrl, new SpiderMan.DownloadCallBack() {
            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onProgress(long total, long downloadedSize) {
                //System.out.println("totalSize:"+total+",downloadedSize:" + downloadedSize);
                System.out.println("%"+((downloadedSize*100)/total));
            }

            @Override
            public void onSucess(String path, String name, long fileSize) {
                System.out.println("下载成功:"+path);
                System.out.println(name);
                System.out.println(fileSize);
            }
        });
          
### 上传
	String uploadUrl = "api/upload";
	spiderMan.upload(uploadUrl, "myfile", new File(Environment.getExternalStorageDirectory()+File.separator+"tmp.apk"),
                new SpiderMan.UploadCallBack() {
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onProgress(long total, long uploadedSize) {
                System.out.println("%"+((uploadedSize*100)/total));
            }

            @Override
            public void onSucess(ResponseBody body) {
                try {
                    System.out.println("上传成功");
                    System.out.println(body.string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
          
### 自定义API Service
	UserApiService userApiService = spiderMan.create(UserApiService.class);