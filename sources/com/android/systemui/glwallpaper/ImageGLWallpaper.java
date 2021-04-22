package com.android.systemui.glwallpaper;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/* access modifiers changed from: package-private */
public class ImageGLWallpaper {
    private static final String TAG = "ImageGLWallpaper";
    private static final float[] TEXTURES = {0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f};
    private static final float[] VERTICES = {-1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f};
    private int mAttrPosition;
    private int mAttrTextureCoordinates;
    private float[] mCurrentTexCoordinate;
    private final ImageGLProgram mProgram;
    private final FloatBuffer mTextureBuffer;
    private int mTextureId;
    private int mUniAod2Opacity;
    private int mUniPer85;
    private int mUniReveal;
    private int mUniTexture;
    private final FloatBuffer mVertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

    ImageGLWallpaper(ImageGLProgram imageGLProgram) {
        this.mProgram = imageGLProgram;
        this.mVertexBuffer.put(VERTICES);
        this.mVertexBuffer.position(0);
        this.mTextureBuffer = ByteBuffer.allocateDirect(TEXTURES.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        this.mTextureBuffer.put(TEXTURES);
        this.mTextureBuffer.position(0);
    }

    /* access modifiers changed from: package-private */
    public void setup(Bitmap bitmap) {
        setupAttributes();
        setupUniforms();
        setupTexture(bitmap);
    }

    private void setupAttributes() {
        this.mAttrPosition = this.mProgram.getAttributeHandle("aPosition");
        this.mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mAttrPosition, 2, 5126, false, 0, (Buffer) this.mVertexBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttrPosition);
        this.mAttrTextureCoordinates = this.mProgram.getAttributeHandle("aTextureCoordinates");
        this.mTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(this.mAttrTextureCoordinates, 2, 5126, false, 0, (Buffer) this.mTextureBuffer);
        GLES20.glEnableVertexAttribArray(this.mAttrTextureCoordinates);
    }

    private void setupUniforms() {
        this.mUniAod2Opacity = this.mProgram.getUniformHandle("uAod2Opacity");
        this.mUniPer85 = this.mProgram.getUniformHandle("uPer85");
        this.mUniReveal = this.mProgram.getUniformHandle("uReveal");
        this.mUniTexture = this.mProgram.getUniformHandle("uTexture");
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    /* access modifiers changed from: package-private */
    public int getHandle(String str) {
        boolean z;
        switch (str.hashCode()) {
            case -2002784538:
                if (str.equals("uTexture")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -1971276870:
                if (str.equals("uAod2Opacity")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -1091770206:
                if (str.equals("uReveal")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case -868354715:
                if (str.equals("uPer85")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 17245217:
                if (str.equals("aTextureCoordinates")) {
                    z = true;
                    break;
                }
                z = true;
                break;
            case 1583025322:
                if (str.equals("aPosition")) {
                    z = false;
                    break;
                }
                z = true;
                break;
            default:
                z = true;
                break;
        }
        if (!z) {
            return this.mAttrPosition;
        }
        if (z) {
            return this.mAttrTextureCoordinates;
        }
        if (z) {
            return this.mUniAod2Opacity;
        }
        if (z) {
            return this.mUniPer85;
        }
        if (z) {
            return this.mUniReveal;
        }
        if (!z) {
            return -1;
        }
        return this.mUniTexture;
    }

    /* access modifiers changed from: package-private */
    public void draw() {
        GLES20.glDrawArrays(4, 0, VERTICES.length / 2);
    }

    private void setupTexture(Bitmap bitmap) {
        int[] iArr = new int[1];
        if (bitmap == null) {
            Log.w(TAG, "setupTexture: invalid bitmap");
            return;
        }
        GLES20.glGenTextures(1, iArr, 0);
        if (iArr[0] == 0) {
            Log.w(TAG, "setupTexture: glGenTextures() failed");
            return;
        }
        GLES20.glBindTexture(3553, iArr[0]);
        GLUtils.texImage2D(3553, 0, bitmap, 0);
        GLES20.glTexParameteri(3553, 10241, 9729);
        GLES20.glTexParameteri(3553, 10240, 9729);
        this.mTextureId = iArr[0];
    }

    /* access modifiers changed from: package-private */
    public void useTexture() {
        GLES20.glActiveTexture(33984);
        GLES20.glBindTexture(3553, this.mTextureId);
        GLES20.glUniform1i(this.mUniTexture, 0);
    }

    /* access modifiers changed from: package-private */
    public void adjustTextureCoordinates(Rect rect, Rect rect2, float f, float f2) {
        this.mCurrentTexCoordinate = (float[]) TEXTURES.clone();
        if (rect == null || rect2 == null) {
            this.mTextureBuffer.put(this.mCurrentTexCoordinate);
            this.mTextureBuffer.position(0);
            return;
        }
        int width = rect.width();
        int height = rect.height();
        int width2 = rect2.width();
        int height2 = rect2.height();
        if (width > width2) {
            float f3 = (float) width;
            float round = ((float) Math.round(((float) (width - width2)) * f)) / f3;
            float f4 = ((float) width2) / f3;
            if (height < height2) {
                f4 *= ((float) height) / ((float) height2);
            }
            if (round + f4 > 1.0f) {
                round = 1.0f - f4;
            }
            int i = 0;
            while (true) {
                float[] fArr = this.mCurrentTexCoordinate;
                if (i >= fArr.length) {
                    break;
                }
                if (i == 2 || i == 4 || i == 6) {
                    this.mCurrentTexCoordinate[i] = Math.min(1.0f, round + f4);
                } else {
                    fArr[i] = round;
                }
                i += 2;
            }
        }
        if (height > height2) {
            float f5 = (float) height;
            float round2 = ((float) Math.round(((float) (height - height2)) * f2)) / f5;
            float f6 = ((float) height2) / f5;
            if (width < width2) {
                f6 *= ((float) width) / ((float) width2);
            }
            if (round2 + f6 > 1.0f) {
                round2 = 1.0f - f6;
            }
            int i2 = 1;
            while (true) {
                float[] fArr2 = this.mCurrentTexCoordinate;
                if (i2 >= fArr2.length) {
                    break;
                }
                if (i2 == 1 || i2 == 3 || i2 == 11) {
                    this.mCurrentTexCoordinate[i2] = Math.min(1.0f, round2 + f6);
                } else {
                    fArr2[i2] = round2;
                }
                i2 += 2;
            }
        }
        this.mTextureBuffer.put(this.mCurrentTexCoordinate);
        this.mTextureBuffer.position(0);
    }

    public void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (this.mCurrentTexCoordinate != null) {
            int i = 0;
            while (true) {
                float[] fArr = this.mCurrentTexCoordinate;
                if (i >= fArr.length) {
                    break;
                }
                sb.append(fArr[i]);
                sb.append(',');
                if (i == this.mCurrentTexCoordinate.length - 1) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                i++;
            }
        }
        sb.append('}');
        printWriter.print(str);
        printWriter.print("mTexCoordinates=");
        printWriter.println(sb.toString());
    }
}
