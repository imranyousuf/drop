package drop.drop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    boolean mPreviewRunning = false;
    Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.hide();

        setContentView(R.layout.activity_main);

        initCamera();
    }

    private void initCamera() {
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void takePicture() {
        mCamera.takePicture(null, null, mPictureCallback);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        //mCamera is an Object of the class “Camera”. In the surfaceCreated we “open” the camera. This is how to start it!!
        mCamera = Camera.open();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mPreviewRunning) {
            mCamera.stopPreview();
        }
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(0);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            mCamera.setDisplayOrientation(90);
        }
        mCamera.setParameters(parameters);
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
        mPreviewRunning = true;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mPreviewRunning = false;
        mCamera.release();
    }

    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] imageData, Camera c) {
        }
    };


}
