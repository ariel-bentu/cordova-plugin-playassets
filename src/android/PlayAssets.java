package bentu.playassets;

import android.util.ArrayMap;

import com.google.android.play.core.assetpacks.AssetPackLocation;
import com.google.android.play.core.assetpacks.AssetPackManager;
import com.google.android.play.core.assetpacks.AssetPackManagerFactory;
import com.google.android.play.core.assetpacks.AssetPackState;
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener;
import com.google.android.play.core.assetpacks.AssetPackStates;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.CallbackContext;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayAssets extends CordovaPlugin {
    public static String TAG = "PlayAssets";
    
    private Map<String,String> playAssets;
    ArrayList<String> pckList;
    private boolean isReady = false;
    private long downloadPercent = 0;
    private long totalSizeToDownload = 0;
    private String currentFileName;
    private int fileIndex = 1;

    private AssetPackManager assetPackManager;

    private interface FileOp {
        void run(JSONArray args) throws Exception;
    }

    public boolean execute(String action, final String rawArgs, final CallbackContext callbackContext) {
        if (action.equals("initPlayAssets")) {
            threadhelper( new FileOp( ){
                public void run(JSONArray args) throws IOException, JSONException {
                    JSONArray names = args.getJSONArray(0);
                    currentFileName = "";
                    if (names != null && names.length() > 0) {
                        pckList = new ArrayList<String>();
                        for (int i = 0; i < names.length(); i++) {
                            pckList.add(names.getString(i));
                        }
                        initAssets();
                    }
                    callbackContext.success();
                }
            }, rawArgs, callbackContext);

        } else if (action.equals("getPlayAssets")) {
            threadhelper( new FileOp( ){
                public void run(JSONArray args) throws IOException, JSONException {
                    JSONObject obj = getPlayAssets();
                    callbackContext.success(obj);
                }
            }, rawArgs, callbackContext);


        } else {
            return false;
        }
        return true;
    }

    private void registerPlayAssets(String name, String path) {
        this.playAssets.put(name, path);
    }

    private void setProgress(long percent, long totalSize, String name) {
        downloadPercent = percent;
        if (percent > 0) {
            totalSizeToDownload = totalSize;
            if (currentFileName.length() == 0) {
                fileIndex = 1;
            } else if (!currentFileName.equalsIgnoreCase(name)) {
                fileIndex++;
            }
            currentFileName = name;
        }
    }

    private void assetsReady() {
        isReady = true;

    }

    private JSONObject getPlayAssets() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("ready", isReady);

//        downloadPercent += 7;
//        if (downloadPercent > 100) {
//            isReady = true;
//        }

        obj.put("downloadPercent", downloadPercent);
        obj.put("totalSizeToDownload", totalSizeToDownload);
        obj.put("fileIndex", fileIndex);


        JSONArray assets = new JSONArray();

        for (Map.Entry<String, String> entry : playAssets.entrySet()) {
            JSONObject asset = new JSONObject();
            asset.put("name", entry.getKey());
            asset.put("path", entry.getValue());
            assets.put(asset);
        }
        obj.put("assets", assets);

        return obj;
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.playAssets = new ArrayMap<String, String>();
        assetPackManager =  AssetPackManagerFactory.getInstance(cordova.getActivity().getApplicationContext());
    }


    private void initAssets() {
        if (pckList == null)
            return;

        downloadPercent = 0;
        ArrayList<String> pckListToFetch = new ArrayList<String>();

        for (int i=0;i<pckList.size();i++) {
            String name = pckList.get(i);
            AssetPackLocation loc = assetPackManager.getPackLocation(name);
            if (loc != null) {
                registerPlayAssets(name, loc.assetsPath());
            } else {
                pckListToFetch.add(name);
            }
        }

        if (pckListToFetch.size() > 0) {
            //actually fetch
            assetPackManager.registerListener(assetPackStateUpdateListener);
            assetPackManager.fetch(pckListToFetch);

            //getPackStates(pckListToFetch);

            assetPackManager.fetch(pckList);
        } else {
            assetsReady();
        }
    }

//    private void getPackStates(List<String> names) {
//        Task<AssetPackStates> aps = assetPackManager.getPackStates(names);
//        aps.
//
//
//
//                .addOnCompleteListener(new OnCompleteListener<AssetPackStates>() {
//                    @Override
//                    public void onComplete(Task<AssetPackStates> task) {
//                        AssetPackStates assetPackStates;
//                        try {
//                            assetPackStates = task.getResult();
//
//                            for (int i=0;i<names.size();i++) {
//                                AssetPackState assetPackState = assetPackStates.packStates().get(names.get(i));
//
//                                if (assetPackState != null) {
//                                    if (assetPackState.status() == AssetPackStatus.WAITING_FOR_WIFI) {
//                                        long totalSizeToDownloadInBytes = assetPackState.totalBytesToDownload();
//                                        if (totalSizeToDownloadInBytes > 0) {
//                                            long sizeInMb = totalSizeToDownloadInBytes / (1024 * 1024);
//                                            if (sizeInMb >= 150) {
//                                                //todo
//                                            }
//                                            assetPackManager.registerListener(assetPackStateUpdateListener);
//                                            assetPackManager.fetch(names);
//                                        }
//                                    }
//                                }
//                            }
//                        } catch (Exception e) {
//                            //Log.d("MainActivity", e.getMessage());
//                        }
//                    }
//                });
//    }

    private AssetPackStateUpdateListener assetPackStateUpdateListener = new AssetPackStateUpdateListener() {
        @Override
        public void onStateUpdate(AssetPackState state) {
            switch (state.status()) {
                case AssetPackStatus.PENDING:
                    break;

                case AssetPackStatus.DOWNLOADING:

                    long downloaded = state.bytesDownloaded();
                    long totalSize = state.totalBytesToDownload();
                    long percent = (long)100.0 * downloaded / totalSize;
                    setProgress(percent, totalSize, state.name());
                    //Log.i(TAG, "PercentDone=" + String.format("%.2f", percent));
                    break;

                case AssetPackStatus.TRANSFERRING:
                    // 100% downloaded and assets are being transferred.
                    // Notify user to wait until transfer is complete.
                    break;

                case AssetPackStatus.COMPLETED:
                    // Asset pack is ready to use. Start the Game/App.
                    initAssets();
                    break;

                case AssetPackStatus.FAILED:
                    // Request failed. Notify user.
                    //Log.e(TAG, String.valueOf(state.errorCode()));
                    break;

                case AssetPackStatus.CANCELED:
                    // Request canceled. Notify user.
                    break;

                case AssetPackStatus.WAITING_FOR_WIFI:
                    //showWifiConfirmationDialog();
                    break;

                case AssetPackStatus.NOT_INSTALLED:
                    // Asset pack is not downloaded yet.
                    break;
                case AssetPackStatus.UNKNOWN:
                    // The Asset pack state is unknown
                    break;
            }

        }
    };

    private void threadhelper(final FileOp f, final String rawArgs, final CallbackContext callbackContext){
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {
                    JSONArray args = new JSONArray(rawArgs);
                    f.run(args);
                } catch ( Exception e) {
                       e.printStackTrace();
                        callbackContext.error("Unknown Error");

                }
            }
        });
    }

}
