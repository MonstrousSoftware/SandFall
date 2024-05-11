package com.monstrous.sandfall;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static com.badlogic.gdx.graphics.GL31.*;


/**
 * Falling Sand demo using compute shader in GLSL
 *
 * by Monstrous Software (May 2024)
 *
 */

public class SandFall extends InputAdapter implements ApplicationListener {
    private final String PROMPT = "PRESS ENTER TO START";

    private SpriteBatch batch;
    private SpriteBatch batchText;
    private ExtendViewport viewport;
    private BitmapFont font;

    private static final int MAX_NUM_CELLS_X = 512;
    private static final int MAX_NUM_CELLS_Y = 512;
    private static final int WORK_GROUP_SIZE_X = 16;
    private static final int WORK_GROUP_SIZE_Y = 16;

    private int computeProgram;
    private Texture[] textures;
    private int readTexIndex;
    private float zoom = 1f;
    private boolean paused = false;
    private boolean started = true;
    private boolean step = false;
    private StringBuffer sb = new StringBuffer();
    private int stepCount = 0;
    private Pixmap pixmapBrush;

    @Override
    public void create() {
        Gdx.app.log("LibGDX version: ", Version.VERSION);
        if (Gdx.gl31 == null) {
            throw new GdxRuntimeException("GLES 3.1 profile required for this programme.");
        }

        batch = new SpriteBatch();
        batchText = new SpriteBatch();
        viewport = new ExtendViewport(MAX_NUM_CELLS_X, MAX_NUM_CELLS_Y);
        viewport.getCamera().position.set(MAX_NUM_CELLS_X/2f, MAX_NUM_CELLS_Y/2f, 0);

        font = new BitmapFont();
        computeProgram = createIterationProgram();

        createTextures();
        initState();

        Gdx.input.setInputProcessor( this );
        pixmapBrush = new Pixmap(Gdx.files.internal("brush.png"));
    }

    @Override
    public void resize(int width, int height) {

        viewport.update(width, height, false);
        batchText.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void render() {
        // process keyboard input
        handleKeys();

        // call compute shader to iterate one step
        if(started && (!paused || step)) {
            computeNextState();
            readTexIndex = 1 - readTexIndex; // switch input and output buffer for next iteration
        }

        // render the texture to the screen
        ScreenUtils.clear(Color.BLACK, false);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.draw(textures[readTexIndex], 0, 0);
        batch.end();



        batchText.begin();
        sb.setLength(0);
        sb.append("FPS: ");
        sb.append(Gdx.graphics.getFramesPerSecond());
        font.draw(batchText, sb.toString() , 0,20);     // show fps
        if(!started) {
            GlyphLayout layout = new GlyphLayout(font, PROMPT);
            int w = Gdx.graphics.getWidth();
            int x = (int)(w - layout.width)/2;
            font.draw(batchText, PROMPT, x, Gdx.graphics.getHeight() * 0.25f);
        }
        batchText.end();

        step = false;
    }

    private void handleKeys(){
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            Gdx.app.exit();
        else if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE))
            paused = !paused;
        else if(Gdx.input.isKeyJustPressed(Input.Keys.S))
            step = true;
        else if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            initState();
            started = false;
            paused = false;
        }
        else if(Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY))
            started = true;
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        batchText.dispose();
        for(Texture tex : textures )
            tex.dispose();
        font.dispose();
       // pixmapBrush.dispose();
    }



    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        started = true;
        addBrushStroke(screenX, screenY);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        addBrushStroke(screenX, screenY);
        return true;
    }

    private void createTextures() {

        textures = new Texture[2];
        for (int i = 0; i < textures.length; i++) {
            Texture tex = new Texture(MAX_NUM_CELLS_X, MAX_NUM_CELLS_Y, Pixmap.Format.RGBA8888);
            tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
            tex.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            textures[i] = tex;
        }
    }

    private void initState() {
        Texture tex = textures[readTexIndex];
        Pixmap pixmap = new Pixmap(tex.getWidth(), tex.getHeight(), Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        tex.draw(pixmap, 0, 0);
        pixmap.dispose();

        pixmap = new Pixmap(Gdx.files.internal("libgdx.png"));
        tex.draw(pixmap, (MAX_NUM_CELLS_X - pixmap.getWidth())/2, 100);
        pixmap.dispose();

        pixmap = new Pixmap(Gdx.files.internal("monstrous.png"));
        tex.draw(pixmap, (MAX_NUM_CELLS_X - pixmap.getWidth())/2, 200);
        pixmap.dispose();
    }

    private void addBrushStroke(int x, int y) {
        Vector3 posVec = new Vector3(x, y, 0);
        viewport.getCamera().unproject(posVec);

        // note: alpha blending doesn't work when drawing a pixmap to texture,
        // so we use a square brush
        Texture tex = textures[readTexIndex];
        tex.draw(pixmapBrush, (int)posVec.x,(int)(tex.getHeight() - posVec.y));
    }


    private static int createShader(String resource, int type, Map<String, String> defines) {
        GL20 gl = Gdx.gl20;

        int shader = gl.glCreateShader(type);
        if (shader == 0) return -1;

        String source = Gdx.files.internal(resource).readString();

        // insert the list of #defines at the line which reads #pragma {{DEFINES}}
        source = source.replace("#pragma {{DEFINES}}",
            defines.entrySet().stream().map(e -> "#define " + e.getKey() + " " + e.getValue()).collect(Collectors.joining("\n")));

        gl.glShaderSource(shader, source);
        Gdx.gl.glCompileShader(shader);

        // check compile status
        IntBuffer intbuf = BufferUtils.newIntBuffer(1);
        gl.glGetShaderiv(shader, GL20.GL_COMPILE_STATUS, intbuf);
        int compiled = intbuf.get(0);
        String log = Gdx.gl.glGetShaderInfoLog(shader);
        if (log.trim().length() > 0)
            System.err.println(log);
        if (compiled == 0)
            throw new AssertionError("Could not compile shader: " + resource);

        return shader;
    }

    private int createIterationProgram()  {
        GL20 gl = Gdx.gl20;

        int program = Gdx.gl.glCreateProgram();
        Map<String, String> defines = new HashMap<>();
        defines.put("WX", WORK_GROUP_SIZE_X + "u");
        defines.put("WY", WORK_GROUP_SIZE_Y + "u");

        int cshader = createShader("shaders/sandfall.cs.glsl", GL_COMPUTE_SHADER, defines);
        if(cshader == -1)
            return -1;
        gl.glAttachShader(program, cshader);
        gl.glLinkProgram(program);
        gl.glDeleteShader(cshader);

        // check link status
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(ByteOrder.nativeOrder());
        IntBuffer intbuf = tmp.asIntBuffer();

        gl.glGetProgramiv(program, GL20.GL_LINK_STATUS, intbuf);
        int linked = intbuf.get(0);

        String programLog = Gdx.gl.glGetProgramInfoLog(program);
        if (programLog.trim().length() > 0)
            System.err.println(programLog);
        if (linked == 0)
            throw new AssertionError("Could not link program");

        computeProgram = program;


        return program;
    }

    private void computeNextState() {
        GL31 gl = Gdx.gl31;
        stepCount++;

        // call compute shader to execute one step of the Game of Life
        // using an input texture and an output texture

        gl.glUseProgram(computeProgram);
        gl.glUniform1i(gl.glGetUniformLocation(computeProgram, "u_step"), stepCount);

        gl.glBindImageTexture(0, textures[readTexIndex].getTextureObjectHandle(), 0, false, 0, GL_READ_ONLY, GL_RGBA8);
        gl.glBindImageTexture(1, textures[1 - readTexIndex].getTextureObjectHandle(), 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);

        int numWorkGroupsX = MAX_NUM_CELLS_X / WORK_GROUP_SIZE_X;
        int numWorkGroupsY = MAX_NUM_CELLS_Y / WORK_GROUP_SIZE_Y;

        gl.glDispatchCompute(numWorkGroupsX, numWorkGroupsY, 1);

        gl.glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT); // wait for computation to complete
    }

}
