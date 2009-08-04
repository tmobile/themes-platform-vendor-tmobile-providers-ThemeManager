package com.tmobile.thememanager.generator;

import com.tmobile.thememanager.R;
import com.tmobile.thememanager.ThemeManager;
import com.tmobile.thememanager.activity.CustomizeColor;
import com.tmobile.thememanager.activity.Customize.Customizations;
import com.tmobile.thememanager.delta_themes.DeltaThemeGenerator;
import com.tmobile.thememanager.provider.PackageResourcesProvider;
import com.tmobile.thememanager.utils.FileUtilities;
import com.tmobile.thememanager.utils.IOUtilities;
import com.tmobile.thememanager.widget.ThemeAdapter.ThemeItem;

import android.app.Activity;
import android.content.Context;
import android.content.pm.BaseThemeInfo;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

/**
 * @deprecated
 *   This code has not been tested!  We moved away from the Bundle-based input
 *   method to using a combination of Customizations and ThemeItem and
 *   a crude attempt was made to get this class compiling again, but it was
 *   never run!
 * @see DeltaThemeGenerator
 */
public final class ThemeGenerator extends AsyncTask<ThemeGenerator.ThemeGenerationParams,
        ThemeGenerator.ThemeGenerationProgress, ThemeGenerator.ThemeGenerationResult> {

    public static final class ThemeGenerationParams {
        public ThemeItem baseTheme;
        public Customizations changes;
        public boolean incremental;
        public BaseThemeInfo.InfoObjectType type;

        public ThemeGenerationParams(ThemeItem baseTheme, Customizations changes,
                boolean incremental, BaseThemeInfo.InfoObjectType type) {
            this.baseTheme = baseTheme;
            this.changes = changes;
            this.incremental= incremental;
            this.type = type;
        }
    }

    public static final class ThemeGenerationProgress {
        public String progressMessage;

        public ThemeGenerationProgress(String msg) {
            progressMessage = msg;
        }
    }

    public static final class ThemeGenerationResult {
        /*
         * Values for what field of messages the component sends
         * asynchronously to the caller.
         */
        public static final int THEME_CREATION_SUCCEEDED = 0;
        public static final int THEME_CREATION_FAILED = 1;

        public String textMessage;
        public int resultCode;
    }

    private class PackageInstallObserver extends IPackageInstallObserver.Stub {
        public void packageInstalled(String name, final int status) {
            synchronized( this) {
                _handler.post(new Runnable() {
                    public void run() {
                        if (status == PackageManager.INSTALL_SUCCEEDED) {
                            Toast.makeText(context, "Theme installed successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Theme installation failed with code=" + status, Toast.LENGTH_LONG).show();
                        }
                        toggleProgressBar(false);
                        cleanUp();
                    }
                });
            }
        }
    }

    private File sdRootFolder;
    private Context context;
    private boolean printResult = true;
    private Handler _handler = new Handler();

    private static final String MANIFEST_HEAD =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:pluto=\"http://www.w3.org/2001/pluto.html\"\n" +
            "    package=\"%s\"\n" + // placeholder for package name
            "    android:hasCode=\"false\"\n" +
            "    android:versionCode=\"1\"\n" +
            "    android:versionName=\"1.0.0\">\n";

    private static final String MANIFEST_TAIL = "    />\n</manifest>\n";

    private static final String THEME_ROOT_FOLDER = "pluto/theme/tmp";
    private static final String THEME_RES_FOLDER = THEME_ROOT_FOLDER + "/res";
    private static final String THEME_ASSETS_FOLDER = THEME_ROOT_FOLDER + "/assets";

    private static final int THUMBNAIL_WIDTH = 78;
    private static final int THUMBNAIL_HEIGHT = 75;

    public ThemeGenerator(Context context) {
        this.context = context;
    }

    public ThemeGenerator.ThemeGenerationResult doInBackground(ThemeGenerator.ThemeGenerationParams... params) {
        ThemeGenerationResult result = new ThemeGenerationResult();
        result.resultCode = ThemeGenerationResult.THEME_CREATION_SUCCEEDED;
        try {
            toggleProgressBar(true);

            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                throw new Exception("SD card either not mounted or not writeable");
            }
            sdRootFolder = Environment.getExternalStorageDirectory();

            // Send message that resource creation started
            publishProgress(new ThemeGenerationProgress("Copy resources..."));

            createLocalDir(THEME_RES_FOLDER);
            createLocalDir(THEME_ASSETS_FOLDER);

            boolean incremental = params[0].incremental;
            parse(params[0].baseTheme, params[0].changes, params[0].type);

            // Send message that resource bundle creation is started
            publishProgress(new ThemeGenerationProgress("Create resource bundle..."));
            runAapt(incremental);

            // Send message that .apk file creation is started
            publishProgress(new ThemeGenerationProgress("Create .apk file..."));
            runApkBuilder();

            // Send message that .apk file installation is started
            publishProgress(new ThemeGenerationProgress("Install .apk file..."));
            String apkPath = getDummyThemePath();
            printResult = false;
            context.getPackageManager()
                    .installPackage(
                            Uri.fromFile(new File(apkPath)),
                            new PackageInstallObserver(),
                            PackageManager.REPLACE_EXISTING_PACKAGE);
        } catch (Exception e) {
            printResult = true;
            result.textMessage = e.getLocalizedMessage();
            result.resultCode = ThemeGenerationResult.THEME_CREATION_FAILED;
        }
        return result;
    }

    @Override
    public void onProgressUpdate(ThemeGenerationProgress... values) {
        ThemeGenerationProgress value = values[0];
        Toast.makeText(context, value.progressMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPostExecute(ThemeGenerator.ThemeGenerationResult result) {
        if (!printResult) {
            return;
        }
        if (result.resultCode == ThemeGenerationResult.THEME_CREATION_SUCCEEDED) {
            Toast.makeText(context, "Theme created successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Theme creation failed!\n" + result.textMessage, Toast.LENGTH_LONG).show();
        }
        toggleProgressBar(false);
        cleanUp();
    }

    private void createLocalDir(String relativePath) throws IOException {
        File folder = new File(sdRootFolder, relativePath);
        folder.mkdirs();
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IOException("Failed to create " + relativePath + " folder");
        }
    }
    
    private static String baseClassName(String cls) {
        int index = cls.lastIndexOf('.');
        if (index <= 0) {
            return cls;
        } else {
            return cls.substring(index + 1);
        }
    }
    
    private void writeAttribute(Writer out, String key, String value) throws IOException {
        out.write(String.format("        pluto:%s=\"%s\"\n", key, value));
    }

    private void parse(ThemeItem baseTheme, Customizations changes, 
            BaseThemeInfo.InfoObjectType type)
            throws IOException, PackageManager.NameNotFoundException {
        File manifest = new File(sdRootFolder, THEME_ROOT_FOLDER + "/AndroidManifest.xml");
        Writer out = new BufferedWriter(new FileWriter(manifest));

        String properName = baseClassName(baseTheme.getPackageName());
        out.write(String.format(MANIFEST_HEAD, "com.tmobile.theme.autogenerated." + properName));

        createLocalDir(THEME_RES_FOLDER + "/values");
        File stylesFile = new File(sdRootFolder, THEME_RES_FOLDER + "/values/themes.xml");
        Writer styles = new BufferedWriter(new FileWriter(stylesFile));
        styles.write("<resources>\n");
        String styleName = properName;
        styles.write(String.format("  <style name=\"%s\">\n", styleName));

        switch (type) {
            case TYPE_THEME:
                out.write("    <theme\n");
                break;

            case TYPE_SOUNDPACK:
                out.write("    <sounds\n");
                break;
        }

        String parentPackage = baseTheme.getPackageName();
        int parentThumbnailId = -1;
        if (baseTheme.type == ThemeItem.TYPE_USER) {
            parentThumbnailId = baseTheme.info.thumbnail;
        }
        String wallpaperFileName = null;
        

//        *        pluto:name="Pluto Default"
//        *        pluto:thumbnail="@drawable/app_thumbnail"
//        *        pluto:author="John Doe"
//        *        pluto:ringtoneFileName="media/audio/ringtone.mp3"
//        *        pluto:notificationRingtoneFileName="media/audio/locked/notification.mp3"
//        *        pluto:copyright="T-Mobile, 2009"
//        *        pluto:wallpaperImage="media/images/wallpaper.jpg"
//        *        pluto:favesBackground="media/images/locked/background.jpg"
//        *        pluto:soundpackName="<package_name>/<sound_pack_name>"
        
        writeAttribute(out, "name", baseTheme.getName());
        writeAttribute(out, "author", baseTheme.getAuthor());
        
        if (changes.wallpaperUri != null) {
            wallpaperFileName = copyResource(changes.wallpaperUri, parentPackage, true);
            writeAttribute(out, "wallpaperImage", wallpaperFileName);
        }

        if (changes.ringtoneUri != null) {
            writeAttribute(out, "ringtoneFileName", 
                    copyResource(changes.ringtoneUri, parentPackage, false));
        }
        
        if (changes.notificationRingtoneUri != null) {
            writeAttribute(out, "notificationRingtoneFileName",
                    copyResource(changes.notificationRingtoneUri, parentPackage, false));
        }
        
        if (changes.colorPaletteName != null) {
            generateStyle(styles, changes.colorPaletteName);
        }

        copyThumbnail(wallpaperFileName, parentPackage, parentThumbnailId, type, out);
        out.write(String.format("        pluto:androidUiStyle=\"@style/%s\"\n", styleName));
        out.write(MANIFEST_TAIL);
        out.close();

        styles.write("  </style>\n");
        styles.write("</resources>\n");
        styles.close();
    }

    private void generateStyle(Writer out, String colorPaletteName)
            throws IOException, PackageManager.NameNotFoundException {
        if (colorPaletteName == null) {
            throw new IOException("Wrong colorPaletteName");
        }
        CustomizeColor.generateStyle(out, colorPaletteName);
    }
    
    private String copyResource(Uri src, String parentPackage, boolean image) 
            throws NameNotFoundException, IOException {
        return copyResource(src.toString(), parentPackage, image);
    }

    private String copyResource(String srcPath, String parentPackage, boolean image)
            throws IOException, PackageManager.NameNotFoundException {
        StringBuffer sb = new StringBuffer("media/");
        if (image) {
            sb.append("images/");
        } else {
            sb.append("audio/");
        }
        createLocalDir(THEME_ASSETS_FOLDER + '/' + sb.toString());
        int index = srcPath.lastIndexOf('/');
        if (index < 0) {
            sb.append(srcPath);
        } else {
            sb.append(srcPath.substring(index + 1));
        }
        String dstPath = sb.toString();
        File dstFile = new File(sdRootFolder, THEME_ASSETS_FOLDER + '/' + dstPath);
        if (srcPath.startsWith("file://") || srcPath.startsWith("content://")) {
            InputStream is = context.getContentResolver().openInputStream(Uri.parse(srcPath));
            try {
                FileUtils.copyToFile(is, dstFile);
            } finally {
                IOUtilities.close(is);
            }
        } else if (parentPackage != null) {
            // Here we ASSUME that the parent is a theme
            AssetManager am = null;
            List<PackageInfo> themePackages = context.getPackageManager().getInstalledThemePackages();
            for (PackageInfo pi : themePackages) {
                if (pi.packageName.equals(parentPackage)) {
                    Resources r = PackageResourcesProvider.getResourcesForTheme(context,
                            parentPackage, pi.applicationInfo.sourceDir,
                            pi.getLockedZipFilePath());
                    am = r.getAssets();
                    break;
                }
            }
            if (am == null) {
                throw new PackageManager.NameNotFoundException("Package " + parentPackage + " is not installed");
            }
            InputStream is = am.open(srcPath);
            FileUtils.copyToFile(is, dstFile);
            is.close();
        } else {
            throw new PackageManager.NameNotFoundException("Illegal name: " + srcPath);
        }
        return dstPath;
    }

    private void copyThumbnail(String wallpaperFileName, String parentPackageName, int parentThumbnailId,
                               BaseThemeInfo.InfoObjectType type, Writer out)
            throws IOException, PackageManager.NameNotFoundException {
        StringBuffer sb = new StringBuffer(THEME_RES_FOLDER);
        sb.append("/drawable");
        createLocalDir(sb.toString());
        sb.append("/thumbnail.png");
        String dstPath = sb.toString();
        File dstFile = new File(sdRootFolder, dstPath);

        // The following logic is used for creating theme thumbnail:
        // 1. if wallpaperFileName is defined, generate thumbnail (78 X 75)
        // 2. else if parentPackageName is defined AND it has thumbnail, use it
        // 3. else use default thumbnail for a given type
        if (wallpaperFileName != null) {
            // case #1
            File wallpaperFile = new File(sdRootFolder, THEME_ASSETS_FOLDER + '/' + wallpaperFileName);
            InputStream is = new FileInputStream(wallpaperFile);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bmp, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, false);
            OutputStream os = new FileOutputStream(dstFile);
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
        } else if (parentPackageName != null && parentThumbnailId != -1) {
            // case #2
            Resources res = context.getPackageManager().getResourcesForApplication(parentPackageName);
            InputStream is = res.openRawResource(parentThumbnailId);
            FileUtils.copyToFile(is, dstFile);
            is.close();
        } else {
            // case #3
            // AFAIK, this should never happen.
            InputStream is = context.getResources()
                    .openRawResource((type == BaseThemeInfo.InfoObjectType.TYPE_SOUNDPACK)?
                            R.drawable.ringtones :
                            R.drawable.themes);
            FileUtils.copyToFile(is, dstFile);
            is.close();
        }
        out.write(String.format("        pluto:thumbnail=\"@drawable/thumbnail\"\n"));
    }

    private void runAapt(boolean incremental)
            throws ClassNotFoundException, RuntimeException {
        StringBuilder sb = new StringBuilder(sdRootFolder.getPath());
        sb.append('/');
        sb.append(THEME_ROOT_FOLDER);
        sb.append('/');
        String themeRootPath = sb.toString();
        String [] args = (incremental) ?
            new String [] {
                "aapt",
                "p",
                "-M",
                themeRootPath +"AndroidManifest.xml",
                "-I",
                "/system/framework/framework-res.apk",
                "-S",
                themeRootPath + "res",
                "-A",
                themeRootPath + "assets",
                "-x",
                "3",
                "-F",
                themeRootPath + "dummy.res",
            } :
            new String [] {
                "aapt",
                "p",
                "-M",
                themeRootPath +"AndroidManifest.xml",
                "-I",
                "/system/framework/framework-res.apk",
                "-S",
                themeRootPath + "res",
                "-A",
                themeRootPath + "assets",
                "-F",
                themeRootPath + "dummy.res",
            };
        /* Functionality removed during rebase project onto android-1.5r2. */        
        int result = 1; // Aapt.invokeAapt(args);
        if (result != 0) {
            throw new RuntimeException("Creation of resource bundle failed!");
        }
    }

    private void runApkBuilder() throws IOException {
        StringBuilder sb = new StringBuilder(sdRootFolder.getPath());
        sb.append('/');
        sb.append(THEME_ROOT_FOLDER);
        sb.append('/');
        String themeRootPath = sb.toString();

        InputStream is = context.getResources().openRawResource(R.raw.approved);
        File keyStore = new File(themeRootPath + "approved.bks");
        FileUtils.copyToFile(is, keyStore);
        is.close();

        /* Functionality removed during rebase project onto android-1.5r2. */
//        ApkBuilder apBuilder = new ApkBuilder();
//        apBuilder.run(new String [] {
//                getDummyThemePath(),
//                "-z",
//                themeRootPath + "dummy.res",
//                "-storepath",
//                themeRootPath + "approved.bks",
//        });
        // Make sure the .apk file is readable by others
        FileUtils.setPermissions(getDummyThemePath(), 0644, -1, -1);
    }

    private String getDummyThemePath() {
        return "data/data/" + ThemeManager.class.getPackage().getName() + "/dummy.apk";
    }

    private void cleanUp() {
        try {
            if (sdRootFolder == null) {
                return;
            }
            File root = new File(sdRootFolder.getPath(), THEME_ROOT_FOLDER);
            FileUtilities.deleteDirectory(root);
        } catch (IOException e) {
            Log.e("com.tmobile.thememanager.generator.ThemeGenerator","Failed to clean", e);
        }
        File apkFile = new File(getDummyThemePath());
        apkFile.delete();
    }

    private void toggleProgressBar(final boolean on) {
        final Activity activity = (Activity)context;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                activity.setProgressBarIndeterminateVisibility(on);
            }
        });
    }

}
