package com.picovr.vrplayer.native3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureHelper {

    private  static  final String TAG="TextureHelper";

    public static int loadTexture(Context context,int resourceId){

        Log.d(TAG,"Enter loadTexture.....");

        final int[] textureObjectIds=new int[1];
        //Reference which generate a texture resource
        GLES20.glGenTextures(1,textureObjectIds,0);
        if (textureObjectIds[0]==0){
            Log.d(TAG,"Failed to generate texture object");
            return 0;
        }

        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inScaled=false;

        Bitmap bitmap=BitmapFactory.decodeResource(context.getResources(),resourceId,options);

        if (bitmap==null){
            Log.d(TAG,"Failed to load bitmap");
            GLES20.glDeleteTextures(1,textureObjectIds,0);
            return 0;
        }

        //Bind texture \ bind reference
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureObjectIds[0]);

        //Set texture properties, filter mode, stretch mode.
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);

        //Send the bitmap to the already bound texture
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0);

        bitmap.recycle();

        //Generate a complete set of mipmaps for the texture images associated with the target
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);

        return textureObjectIds[0];
    }

    public static int loadTextur3D(Context context){

        final int[] textures = new int[1];
        //Generate a texture to textures, and return a non-zero value if the generation is successful
        GLES20.glGenTextures(1, textures, 0);

        if (textures[0]==0){
            Log.d(TAG,"Failed to generate texture object");
            return 0;
        }
        //Bind the texture we just generated to OpenGL 3D texture, and tell OpenGL that this is a 3D texture
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);

        //Used to set texture filtering method, GL_TEXTURE_MIN_FILTER is the filtering method when zooming out, GL_TEXTURE_MAG_FILTER is zooming in
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        return textures[0];
    }

}
