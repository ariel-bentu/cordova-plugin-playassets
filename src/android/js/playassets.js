cordova.define("cordova-plugin-assets.PlayAssets", function (require, exports, module) {
    /**
     * @module PlayAssets
     */
    module.exports = {

        initPlayAssets: (names) => {
            cordova.exec(
                () => { },
                (err) => { },

                "PlayAssets", "initPlayAssets", [names]);
        },
        getPlayAssets: (callback) => {
            cordova.exec(
                (res) => callback(res),
                (err) => {callback(undefined)},

                "PlayAssets", "getPlayAssets", []);
        }

    };
});
