package com.picovr.vrplayer.native3d;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import com.picovr.vractivity.Eye;
import com.picovr.vractivity.HmdState;
import com.picovr.vractivity.RenderInterface;
import com.picovr.vractivity.VRActivity;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class VR3DNativeActivity extends VRActivity implements RenderInterface, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG_TH = "PicoVRPlayerActivity";
    private static final String DBG_LC = "LifeCycle :";

    private static final float CAMERA_Z = 0.01f;
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 1000.0f;

    private SurfaceTexture surfaceTexture;
    private MediaPlayer mediaPlayer;
    private Context context;

    //vertex buffer
    private FloatBuffer playerVertices;
    private FloatBuffer playerLeft;
    private FloatBuffer playerRight;

    private int textureId;
    private int programId;

    private int aPositionHandle;
    private int uMatrixHandle;
    private int uTextureSamplerHandle;
    private int uSTMMatrixHandle;
    private int aTextureCoordHandle;

    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelPlayer;
    private float[] headOritation;
    private float[] modelView;
    private float[] modelViewProjection;
    private float[] mSTMatrix;

    private boolean playerPrepared;
    private boolean updateSurface;
    //Vertex array
    public static final float[] PLAYER_COORDSY = new float[]{
            200, 200, 0,
            -200, 200, 0,
            200, -200, 0,
            -200, -200, 0,
    };
    //Left eye texture array
    public static final float[] PLAYER_LEFT = new float[]{
            0.5f, 1f,
            0f, 1f,
            0.5f, 0f,
            0f, 0f
    };
    //Right eye texture array
    public static final float[] PLAYER_RIGHT = new float[]{
            1f, 1f,
            0.5f, 1f,
            1f, 0f,
            0.5f, 0f
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG_TH, DBG_LC + "onCreate BEGIN");
        super.onCreate(savedInstanceState);

        this.context = this;
        camera = new float[16];
        view = new float[16];
        modelPlayer = new float[16];
        headOritation = new float[4];
        modelViewProjection = new float[16];
        modelView = new float[16];
        headView = new float[16];
        mSTMatrix = new float[16];

        playerPrepared = false;
        synchronized (this) {
            updateSurface = false;
        }
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.test3d;

        Log.e(TAG_TH, "videoPath = " + videoPath);

        //MediaPlayer initialization
        mediaPlayer = new MediaPlayer();
        try {
            //MediaPlayer gets video resources
            mediaPlayer.setDataSource(this, Uri.parse(videoPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Set the type of streaming media
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //Set loop play
        mediaPlayer.setLooping(true);

        if (!playerPrepared) {
            try {
                mediaPlayer.prepare();
                playerPrepared = true;
            } catch (IOException t) {
                Log.e(TAG_TH, "media player prepare failed");
            }
        }

        Log.d(TAG_TH, DBG_LC + "onCreate END");
    }

    @Override
    public void initGL(int width, int height) {

        Log.d(TAG_TH, DBG_LC + "initGL BEGIN");

        // Dark background so text shows up well.
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f);

        //Allocate memory
        ByteBuffer bbPlayerVertices = ByteBuffer.allocateDirect(PLAYER_COORDSY.length * 4);

        //Set the storage order to nativeOrder
        bbPlayerVertices.order(ByteOrder.nativeOrder());
        playerVertices = bbPlayerVertices.asFloatBuffer();
        //Add PLAYER_COORDSY
        playerVertices.put(PLAYER_COORDSY);
        playerVertices.position(0);

        ByteBuffer bbPlayerRight = ByteBuffer.allocateDirect(PLAYER_RIGHT.length * 4);
        bbPlayerRight.order(ByteOrder.nativeOrder());
        playerRight = bbPlayerRight.asFloatBuffer();
        playerRight.put(PLAYER_RIGHT);
        playerRight.position(0);

        ByteBuffer bbPlayerLeft = ByteBuffer.allocateDirect(PLAYER_LEFT.length * 4);
        bbPlayerLeft.order(ByteOrder.nativeOrder());
        playerLeft = bbPlayerLeft.asFloatBuffer();
        playerLeft.put(PLAYER_LEFT);
        playerLeft.position(0);

        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.vertex_shader);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.fragment_shader);
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        ShaderUtils.checkGlError("programId program params");

        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        uMatrixHandle = GLES20.glGetUniformLocation(programId, "uMatrix");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        ShaderUtils.checkGlError("Handle program params");

        textureId = TextureHelper.loadTextur3D(this);

        //SurfaceTexture is to get data of a new frame from the video stream and the camera data stream. Use updateTexImage to get the new data.
        //Use textureId to create a SurfaceTexture
        surfaceTexture = new SurfaceTexture(textureId);
        //Listening for a new frame data
        surfaceTexture.setOnFrameAvailableListener(this);
        //Use surfaceTexture to create a Surface
        Surface surface = new Surface(surfaceTexture);
        //Set the surface as the output surface of the mediaPlayer
        mediaPlayer.setSurface(surface);
        surface.release();
        mediaPlayer.start();

        Matrix.setIdentityM(modelPlayer, 0);
        Matrix.translateM(modelPlayer, 0, 0.0f, .0f, -500f);

        //Set up camera
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        GLES20.glViewport(0, 0,width ,height );
        
        Log.d(TAG_TH, DBG_LC + "initGL END");
    }

    @Override
    public void deInitGL() {

    }


    @Override
    public void onFrameBegin(HmdState hmdState) {
        Log.d(TAG_TH, DBG_LC + "onFrameBegin BEGIN");

        //Get the camera rotation array
        hmdState.getOrientation(headOritation, 0);

        quternion2Matrix(headOritation, headView);


        Matrix.invertM(headView, 0, headView, 0);

        ShaderUtils.checkGlError("onReadyToDraw");
        Log.d(TAG_TH, DBG_LC + "onFrameBegin END");

    }

    @Override
    public void onDrawEye(Eye eye) {

        Log.d(TAG_TH, DBG_LC + "onDrawEye BEGIN");
      
        //Turn on depth detection
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        //Disable clipping test
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        //*Set clipping window
        GLES20.glScissor(1, 1, 1278, 1278);
        //Set the screen background color RGBA
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        //Clean the color and depth buffers
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        ShaderUtils.checkGlError("colorParam");

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, headView, 0, camera, 0);
        // Build the ModelView and ModelViewProjection matrices
        // Calculate cube position and light.
        float[] perspective = new float[16];

        getPerspective(Z_NEAR, Z_FAR, 51.f, 51.f, 51.f, 51.f, perspective, 0);

         //player window not following camera
//        Matrix.multiplyMM(modelView, 0, view, 0, modelPlayer, 0);
        //player window following camera
        Matrix.multiplyMM(modelView, 0, camera, 0, modelPlayer, 0);

        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);

        synchronized (this) {
            if (updateSurface) {
                //Update the texture
                surfaceTexture.updateTexImage();
                //Make the new texture and texture coordinate correspond correctly
                surfaceTexture.getTransformMatrix(mSTMatrix);
                updateSurface = false;
            }
        }
        //Use shader
        GLES20.glUseProgram(programId);
        // Set ModelView, MVP, position, and color.
        //Send transformation matrix to shader（
        GLES20.glUniformMatrix4fv(uMatrixHandle, 1, false, modelViewProjection, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);
        ShaderUtils.checkGlError("drawing uMatrixHandle");

        //Enable vertex array
        playerVertices.position(0);
        //Send vertex position data to shader
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, 12, playerVertices);
        //Enable vertex coordinate array
        GLES20.glEnableVertexAttribArray(aPositionHandle);

        //0:left eye, 1:right eye
        if (eye.getType() == 0) {
            playerLeft.position(0);
            GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, playerLeft);
            GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        } else {
            playerRight.position(0);
            GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, playerRight);
            GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        }

        //Use triangle strip to draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Deactivate the vertex array
        GLES20.glDisableVertexAttribArray(aPositionHandle);
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle);
        GLES20.glDisable(GLES20.GL_BLEND);
        ShaderUtils.checkGlError("drawing player");

        Log.d(TAG_TH, DBG_LC + "onDrawEye END");
    }

    @Override
    public void onFrameEnd() {

    }

    @Override
    public void onTouchEvent() {

    }

    @Override
    public void onRenderPause() {

    }

    @Override
    public void onRenderResume() {

    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void renderEventCallBack(int i) {

    }

    @Override
    public void surfaceChangedCallBack(int i, int i1) {

    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Listen for a new frame
        updateSurface = true;
    }


    public void quternion2Matrix(float Q[], float M[]) {
        float x = -1.0f * Q[0];
        float y = -1.0f * Q[1];
        float z = -1.0f * Q[2];
        float w = Q[3];
        float ww = w * w;
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;

        M[0] = (ww + xx - yy - zz);
        M[1] = 2 * (x * y - w * z);
        M[2] = 2 * (x * z + w * y);
        M[3] = 0.f;

        M[4] = 2 * (x * y + w * z);
        M[5] = (ww - xx + yy - zz);
        M[6] = 2 * (y * z - w * x);
        M[7] = 0.f;

        M[8] = 2 * (x * z - w * y);
        M[9] = 2 * (y * z + w * x);
        M[10] = (ww - xx - yy + zz);
        M[11] = 0.f;

        M[12] = 0.0f;
        M[13] = 0.0f;
        M[14] = 0.0f;
        M[15] = 1.f;
    }

    public void getPerspective(float near, float far, float left, float right, float bottom, float top, float[] perspective, int offset) {
        Log.d(TAG_TH, DBG_LC + "getPerspective BEGIN");
        float l = (float) (-Math.tan(Math.toRadians((double) left))) * near;
        float r = (float) Math.tan(Math.toRadians((double) right)) * near;
        float b = (float) (-Math.tan(Math.toRadians((double) bottom))) * near;
        float t = (float) Math.tan(Math.toRadians((double) top)) * near;
        Matrix.frustumM(perspective, offset, l, r, b, t, near, far);
        Log.d(TAG_TH, DBG_LC + "getPerspective END");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            mediaPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying())
            mediaPlayer.start();
    }

}
