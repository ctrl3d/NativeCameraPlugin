package work.ctrl3d.camerax

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20

class UnityTextureBridge(private val width: Int, private val height: Int) {

    lateinit var surfaceTexture: SurfaceTexture
        private set
    private var textureId = 0
    @Volatile private var frameAvailable = false

    fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(textureId).apply {
            setDefaultBufferSize(width, height)
            setOnFrameAvailableListener { frameAvailable = true }
        }
        return textureId
    }

    fun update(): Boolean {
        if (!frameAvailable) return false
        surfaceTexture.updateTexImage()
        frameAvailable = false
        return true
    }

    fun release() {
        surfaceTexture.release()
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }
}
