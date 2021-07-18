# cordova-plugin-playassets

This is a cordova plugin, to help managed [Google Play Assets](https://developer.android.com/guide/playcore/asset-delivery) in a cordova based application.


The plugin offers two javascript methods:
- `initPlayAssets`
  - With this method you initiate the download
  - It accepts an array of strings, which are the asset names of you app
  - returns nothing

- `getPlayAssets`
  - returns an object:
  ```
  {
      "ready": true/false,
      "downloadPercent": 0-100,
      "totalSizeToDownload": in bytes of the current asset being downloaded,
      "fileIndex": integer, the index of the file being downloaded,
      "assets":[{
          "name": asset name,
          "path": asset's root path
      }, ...
      ]
  }
  ```

  You should call `getPlayAssets`, until its `ready` is true. at that point all assets are available.

  >Note: only relevant for Android, as the Google Play Asset is a google concept not relevant for iOS.
  
