package com.example.softwarelab.androidbasedfem.Rendering;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Utils.LoggerConfig;
import com.example.softwarelab.androidbasedfem.Utils.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_LINE_STRIP;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glBindAttribLocation;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.orthoM;
import static android.opengl.Matrix.translateM;
import static android.support.constraint.Constraints.TAG;
import static android.view.View.generateViewId;
import static javax.microedition.khronos.opengles.GL10.GL_FLOAT;


public class Renderer implements GLSurfaceView.Renderer {
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 2;
    private final Context context;
    private int program;
    private int program_nonuni;

    // Coordinate System Data
    private FloatBuffer CoordinateSystemData;

    // View Matrices
    // With them you can change the openGL view ( Check: Kevin Brothaler, OpenGL ES 2 for Android )
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];

    // Pointer list to store Geometries
    private List<Triangle> Triangles = new ArrayList<Triangle>();

    private List<Rectangle> Rectangles = new ArrayList<Rectangle>();

    private List<Triangle> TrianglesDeformed = new ArrayList<Triangle>();
    private List<BoundaryCondition> BoundaryCondition = new ArrayList<BoundaryCondition>();

    // Shader information ( Check: Kevin Brothaler, OpenGL ES 2 for Android )
    private static final String U_MATRIX = "u_Matrix";
    private int uMatrixLocation;
    private static final String U_COLOR = "u_Color";
    private int uColorLocation;
    private int aColorLocation;

    private static final String A_POSITION = "a_Position";
    private static final String A_COLOR = "a_Color";

    private int aPositionLocation;

    // View Parameters
    private float aspectRatio;
    private float viewScale = 1.0f;
    private float xOffset = 0.0f;
    private float yOffset = 0.0f;
    // Max Parameters
    float xmin = 10000000.0f;
    float xmax = -10000000.0f;
    float ymin = 10000000.0f;
    float ymax = -10000000.0f;

    private float maxDisplacement = 0.0f;
    private float minDisplacement = 0.0f;

    private float maxStress = 0.0f;
    private float minStress = 0.0f;

    private float maxStrain = 0.0f;
    private float minStrain = 0.0f;


    private colorBar Bar;
    private int scalingFactor = 1;

    // 0 - Displacement; 1 - vonMisesStress; 2 - vonMisesStrain
    private int choosenField = 0;

    private boolean animation = false;
    private boolean showColor = false;
    private boolean showDisp = false;

    // ScaleGestureDetector to handle scaling
    public ScaleGestureDetector mScaleDetector;

    // Constructor
    // Allocates standard coordinate system
    // Allocates ScaleGestureDetector
    public Renderer(Context context) {
        this.context = context;

        float[] tableVertices = {

                -1.0f, 0.0f,

                1.0f, 0.0f,

                0.0f, -1.0f,

                0.0f, 1.0f};

        CoordinateSystemData = ByteBuffer

                .allocateDirect(tableVertices.length * BYTES_PER_FLOAT)

                .order(ByteOrder.nativeOrder())

                .asFloatBuffer();

        CoordinateSystemData.put(tableVertices);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());


    }

    // This function is called when openGL view is constructed
    // Compiles shaders and links them to the actual program ( Check: Kevin Brothaler, OpenGL ES 2 for Android )
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        // Define background color
        glClearColor(0.658824f, 0.65824f, 0.658824f, 0.0f);

        setUpShaderProgram();
        setUpShaderProgramNonuniform();

    }

    // Nonuniform shader is needed for nonuniform color distribution (Displacement, stress field)
    public void setUpShaderProgramNonuniform(  )
    {

        String vertexShaderSource = TextResourceReader
                    .readTextFileFromResource(context, R.raw.vertex_shader_nonuniform);

        String fragmentShaderSource = TextResourceReader
                    .readTextFileFromResource(context, R.raw.fragment_shader_nonuniform);

        // Compile Shaders
        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
        // Link Shaders to program

        glAttachShader(program_nonuni, vertexShader);
        glAttachShader(program_nonuni, fragmentShader);

        glBindAttribLocation(program_nonuni, 0 , A_POSITION);
        glBindAttribLocation(program_nonuni, 1, A_COLOR);

        program_nonuni = ShaderHelper.linkProgram(vertexShader, fragmentShader);
        // Check if successful
        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program_nonuni);
        }

    }

    public void setUpShaderProgram(  ) {

        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.vertex_shader);

        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.fragment_shader);

        // Compile Shaders
        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
        // Link Shaders to program

        glBindAttribLocation(program, 0 , A_POSITION);
        glBindAttribLocation(program, 1, A_COLOR);

        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);
        // Check if successful
        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        glUseProgram(program);
        uColorLocation = glGetUniformLocation(program, U_COLOR);
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);

    }

    // This function is called every time the view changes, e.g. changing from landscape to portrait or vice versa
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);

        // Check if we are in landscape or portrait mode
        aspectRatio = width > height ?

                (float) width / (float) height :

                (float) height / (float) width;

        if (width > height) {
            // Landscape
            orthoM(projectionMatrix, 0, -aspectRatio * viewScale, aspectRatio * viewScale, -1f * viewScale, 1f * viewScale, -1f * viewScale, 1f * viewScale);

        } else {
            // Portrait
            orthoM(projectionMatrix, 0, -1f * viewScale + viewScale * xOffset, 1f * viewScale + viewScale * xOffset, -aspectRatio * viewScale + viewScale * yOffset, aspectRatio * viewScale + viewScale * yOffset, -1f * viewScale, 1f * viewScale);
        }
    }

    // This function called automatically every time something should be rendered
    // Updates the view matrices ( Check: Kevin Brothaler, OpenGL ES 2 for Android )
    // Calls the functions to draw the coordinate system and the geometries
    @Override
    public void onDrawFrame(GL10 gl10) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);

        orthoM(projectionMatrix, 0, -1f * viewScale + viewScale * xOffset, 1f * viewScale + viewScale * xOffset, -aspectRatio * viewScale + viewScale * yOffset, aspectRatio * viewScale + viewScale * yOffset, -1f * viewScale, 1f * viewScale);


        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);

        // Update the viewProjection matrix, and create an inverted matrix for
        // touch picking.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0,
                viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        // As only one shader program can bes used, the coordinate system needs drawn with the active program (uniform/nonuniform)
        if( showColor || animation || showDisp )
        {
            DrawCoordinateSystem_nonuni();
        }
        else{
            DrawCoordinateSystem();
        }

        int lengthTri = Triangles.size();

        if ( Bar != null )
        {
            Bar.DrawColorBar(this);
        }


        if (showDisp || animation )
        {
            float interval = 10;

            if ( animation )
            {
                long time =  SystemClock.uptimeMillis() % 4000L;
                // Modulus Division by 10
                interval = ( 0.005f*((int) time)) % 10;
            }

            Triangle tmpTriangle = new Triangle();
            float[] tmpVertices = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
            for (int i = 0; i < lengthTri ; i++) {
                for (int j = 0; j < 6; j++) {

                    float originalConfiguration = Triangles.get(i).getVertices().get(j);

                    float deformedDisp = Triangles.get(i).getDisp(j);

                    // Copy as array
                    tmpTriangle.setDisp( j, (float) Triangles.get(i).getDisp(j) );

                    tmpVertices[j] = originalConfiguration + (deformedDisp * ((float)scalingFactor) / 10 * interval );
                }

                tmpTriangle.setVertexData( tmpVertices );
                if( choosenField == 0)
                {
                    tmpTriangle.setColorData( 0, maxDisplacement, minDisplacement );
                } else if( choosenField == 1)
                {
                    for( int j = 0; j < 3; j++)
                    {
                        tmpTriangle.setVonMisesStressAveragedToNodes(j, (float) Triangles.get(i).getVonMidesStressAveragedToNodes(j));
                    }
                    tmpTriangle.setColorData( 1, maxStress, minStress );
                }else if( choosenField == 2)
                {
                    for( int j = 0; j < 3; j++)
                    {
                        tmpTriangle.setVonMisesStrainAveragedToNodes(j, (float) Triangles.get(i).getVonMidesStrainAveragedToNodes(j));
                    }
                    tmpTriangle.setColorData( 2, maxStrain, minStrain );
                }

                tmpTriangle.DrawWithColor(this);
            }
        }else if( showColor )
            {
                //Bar.DrawColorBar(this);
                for (int i = 0; i < lengthTri; i++) {
                    Triangles.get(i).DrawWithColor(this);
                }

            } else
                {
                    // Loop over all triangles
                    for (int i = 0; i < lengthTri; i++) {
                        Triangles.get(i).Draw(this);
                    }

                    // Loop over all Rectangles
                    int lengthRec = Rectangles.size();
                    for (int i = 0; i < lengthRec; i++) {
                        Rectangles.get(i).Draw(this);
                    }

                    // Loop over all LoadArrows
                    int lengthLoadArrows = BoundaryCondition.size();
                    for (int i = 0; i < lengthLoadArrows; i++) {
                        BoundaryCondition.get(i).Draw(this);
                    }
                }
    }

    public void setMaxMinValues()
    {
        int lengthRec = Rectangles.size();
        int lengthTri = Triangles.size();
        FloatBuffer verticesRec;
        FloatBuffer verticesTri;

        // Loop over all Rectangles
        for (int i = 0; i < lengthRec; i++) {
            verticesRec = Rectangles.get(i).getVertices();
            for (int j = 0; j < 8; j = j + 2) {
                // Loop over x-value and find min and max
                if (verticesRec.get(j) < xmin) {
                    xmin = verticesRec.get(j);
                }
                if (verticesRec.get(j) > xmax) {
                    xmax = verticesRec.get(j);
                }
                // Loop over y- values and find min and max
                if (verticesRec.get(j + 1) < ymin) {
                    ymin = verticesRec.get(j + 1);
                }
                if (verticesRec.get(j + 1) > ymax) {
                    ymax = verticesRec.get(j + 1);
                }
            }
        }
        maxDisplacement = 0.0f;
        minDisplacement = 10000000000000.0f;
        maxStress = 0.0f;
        minStress = 1e15f;
        maxStrain = 0.0f;
        minStrain = 1000000000000.0f;
        // Loop over all Triangles
        for (int i = 0; i < lengthTri; i++) {
            verticesTri = Triangles.get(i).getVertices();
            for (int j = 0; j < 6; j = j + 2) {
                // Loop over x-value and find min and max
                if (verticesTri.get(j) < xmin) {
                    xmin = verticesTri.get(j);
                }
                if (verticesTri.get(j) > xmax) {
                    xmax = verticesTri.get(j);
                }
                // Loop over y- values and find min and max
                if (verticesTri.get(j + 1) < ymin) {
                    ymin = verticesTri.get(j + 1);
                }
                if (verticesTri.get(j + 1) > ymax) {
                    ymax = verticesTri.get(j + 1);
                }

                float disp = (float) Math.sqrt(  Math.pow(Triangles.get(i).getDisp(j),2) + Math.pow(Triangles.get(i).getDisp(j + 1),2) );
                if (maxDisplacement < disp)
                {
                    maxDisplacement = disp;
                }

                if (minDisplacement > disp)
                {
                    minDisplacement = disp;
                }
            }

            for( int j = 0; j < 3; j++)
            {
                float stress = Triangles.get(i).getVonMidesStressAveragedToNodes(j);
                float strain = Triangles.get(i).getVonMidesStrainAveragedToNodes(j);
                if (maxStress < stress)
                {
                    maxStress = stress;
                }

                if (minStress > stress)
                {
                    minStress = stress;
                }

                if (maxStrain < strain)
                {
                    maxStrain = strain;
                }

                if (minStrain > strain)
                {
                    minStrain = strain;
                }
            }

        }
    }


    public void setFieldColor(int field )
    {
        float max = 0.0f;
        float min = 0.0f;

        choosenField = field;

        if ( field == 0 )
        {
            max = maxDisplacement;
            min = minDisplacement;
        } else if (field == 1 )
        {
            max = maxStress;
            min = minStress;
        } else if( field == 2 )
        {
            max = maxStrain;
            min = minStrain;
        }
        int lengthTri = Triangles.size();
        for (int i = 0; i < lengthTri ; i++) {

            Triangles.get(i).setColorData( field, max, min );
        }
    }

    public void setShowColor( boolean show )
    {
        showColor = show;
    }

    public void setAnimation( boolean anim )
    {
        animation = anim;
    }

    public void setShowDisp( boolean show )
    {
        showDisp = show;
    }

    // This function will update the Coordinate System according to the view size
    public void updateCoordinateSystem() {
        float[] tableVertices = {

                // x_Axis

                (-1.0f + xOffset) * viewScale, 0.0f,

                (1.0f + xOffset) * viewScale, 0.0f,

                // y-Axis

                0.0f, (-1.0f + yOffset) * viewScale,

                0.0f, (1.0f + yOffset) * viewScale};

        CoordinateSystemData = ByteBuffer

                .allocateDirect(tableVertices.length * BYTES_PER_FLOAT)

                .order(ByteOrder.nativeOrder())

                .asFloatBuffer();

        CoordinateSystemData.put(tableVertices);
    }

    public void updateScalingFactor(int i)
    {
        scalingFactor = i;
    }

    public void updateColorBar()
    {

        Bar.updateColorBar( xOffset, yOffset, viewScale );
    }

    // These functions add the new geometries to the pointer list
    // After that geometries will directly appear on the screen, as onDrawFrame will call the corresponding drawing function
    public void addTriangleToRenderer(Triangle triangle, boolean deformed) {
        if (deformed == true)
        {
            TrianglesDeformed.add(triangle);
        }
        else
        {
            Triangles.add(triangle);
        }

    }

    public void addBoundaryConditionToRenderer( BoundaryCondition boundary ) { BoundaryCondition.add( boundary ); }

    public void addRectangleToRenderer(Rectangle rectangle) {
        Rectangles.add(rectangle);
    }

    public void addColorBartoRenderer( )
    {
        // Define height manually, screenheight can not be obtained at that time
        Bar = new colorBar( xOffset, yOffset, viewScale, 1.3f );
    }


    // These functions clear the pointer lists
    public void clearFromRenderer() {
        Triangles = new ArrayList<Triangle>();
        Rectangles = new ArrayList<Rectangle>();
    }
    public void removeTriangleFromRenderer(int i) { Triangles.remove(i); }
    public void removeRectanglesFromRenderer(int i) { Rectangles.remove(i); }

    // This function listens to multitouch events and updates the viewScale when user zooms on the screen
    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            viewScale /= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            //viewScale = Math.max(0.1f, Math.min(viewScale, 5.0f));

            return true;
        }
    }

    // this function enables to change the viewOffset
    public void setViewOffset(float dx, float dy) {
        xOffset = dx;
        yOffset = dy;
    }

    // This function loops over all vertices, finds the min and max values and scales the view accordingly
    public void centerView() {

        //if ((ymax - ymin) > aspectRatio * (xmax - xmin)) {
        //    viewScale = (ymax - ymin);
        //} else {
        //    viewScale = (xmax - xmin);
        //}
        int lengthTri = Triangles.size();
        int lengthRec = Rectangles.size();

        viewScale = (xmax - xmin) * 0.7f;
        if (lengthRec > 0 || lengthTri > 0) {
            xOffset = ((xmax + xmin) / 2.0f) / viewScale;
            yOffset = ((ymax + ymin) / 2.0f) / viewScale;
        }

        updateCoordinateSystem();

        if( Bar != null )
        {
            updateColorBar(  );
        }
    }

    // This function checks if a point is inside or outside of a geometry
    // If point is inside, the color of the corresponding element will be changed
    public int[] insideOutsideTest(float[] Point) {
        FloatBuffer vertices;
        float xmin;
        float xmax;
        float ymin;
        float ymax;

        // Loop over all Rectangles
        int lengthRec = Rectangles.size();
        for (int i = 0; i < lengthRec; i++) {
            vertices = Rectangles.get(i).getVertices();
            xmin = vertices.get(0);
            xmax = vertices.get(0);
            ymin = vertices.get(1);
            ymax = vertices.get(1);

            for (int j = 2; j < 8; j = j + 2) {
                // Loop over x-value and find min and max
                if (vertices.get(j) < xmin) { xmin = vertices.get(j); }
                if (vertices.get(j) > xmax) { xmax = vertices.get(j); }

                // Loop over y- values and find min and max
                if (vertices.get(j+1) < ymin) { ymin = vertices.get(j+1); }
                if (vertices.get(j+1) > ymax) { ymax = vertices.get(j+1); }
            }

            // If inside change color
            if (Point[0] > xmin && Point[0] < xmax && Point[1] > ymin && Point[1] < ymax) {
                Rectangles.get(i).changeColor(3);
                int[] res = {0, i};
                return res;
            }
        }

        // Loop over all Triangles
        int lengthTri = Triangles.size();
        for (int i = 0; i < lengthTri; i++) {
            boolean b1, b2, b3;
            vertices = Triangles.get(i).getVertices();
            float[] p1 = {vertices.get(0), vertices.get(1)};
            float[] p2 = {vertices.get(2), vertices.get(3)};
            float[] p3 = {vertices.get(4), vertices.get(5)};

            b1 = sign(Point, p1, p2) < 0.0f;
            b2 = sign(Point, p2, p3) < 0.0f;
            b3 = sign(Point, p3, p1) < 0.0f;

            // If inside change color
            if ((b1 == b2) && (b2 == b3)) {
                Triangles.get(i).changeColor(1);
                int[] res = {1, i};
                return res;
            }
        }
        int[] res = {-1, -1};
        return res;
    }

    // This function is only used for the triangle inside outside check
    private float sign(float[] p1, float[] p2, float[] p3) {
        return ((p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1]));
    }

    // This function checks if vertex is close to an already existing vertex
    // If true, new vertex takes same values as existing one
    public float[] IntersectionTest(float[] point) {
        int lengthRec = Rectangles.size();
        float distance;
        FloatBuffer vertices;

        // Loop over all Rectangles
        for (int i = 0; i < lengthRec; i++) {
            vertices = Rectangles.get(i).getVertices();
            for (int j = 0; j < 8; j = j + 2) {
                Log.d("Info", "Befor vertices");
                distance = (float) Math.sqrt(Math.pow((point[0] - vertices.get(j)), 2) + Math.pow((point[1] - vertices.get(j + 1)), 2));
                Log.d("Distance", String.valueOf(distance));
                if (distance < 0.1 * viewScale) {
                    float newVertice[] = {vertices.get(j), vertices.get(j + 1)};
                    Log.d(TAG, "Jump on Vertice");
                    return newVertice;
                }
            }

            float midPointX1 = (vertices.get(0) + vertices.get(2)) / 2;
            float midPointY1 = (vertices.get(1) + vertices.get(3)) / 2;

            float midPointX2 = (vertices.get(2) + vertices.get(6)) / 2;
            float midPointY2 = (vertices.get(3) + vertices.get(7)) / 2;

            float midPointX3 = (vertices.get(6) + vertices.get(4)) / 2;
            float midPointY3 = (vertices.get(7) + vertices.get(5)) / 2;

            float midPointX4 = (vertices.get(4) + vertices.get(0)) / 2;
            float midPointY4 = (vertices.get(5) + vertices.get(1)) / 2;

            float[] midPointX = {midPointX1, midPointX2, midPointX3, midPointX4};
            float[] midPointY = {midPointY1, midPointY2, midPointY3, midPointY4};

            for (int j = 0; j < 4; j++) {
                distance = (float) Math.sqrt(Math.pow((point[0] - midPointX[j]), 2) + Math.pow((point[1] - midPointY[j]), 2));
                if (distance < 0.1 * viewScale) {
                    float newVertice[] = {midPointX[j], midPointY[j]};
                    Log.d(TAG, "Jump on MidPoint");
                    return newVertice;
                }
            }
        }

        // Loop over all Triangles
        int lengthTri = Triangles.size();
        for (int i = 0; i < lengthTri; i++) {
            vertices = Triangles.get(i).getVertices();
            for (int j = 0; j < 6; j = j + 2) {
                distance = (float) Math.sqrt(Math.pow((point[0] - vertices.get(j)), 2) + Math.pow((point[1] - vertices.get(j + 1)), 2));
                if (distance < 0.1 * viewScale) {
                    float newVertice[] = {vertices.get(j), vertices.get(j + 1)};
                    Log.d(TAG, "Jump on Vertice of Triangle");
                    return newVertice;
                }
            }
        }
        point = isOnCoordinateAxis(point);

        return point;

    }

    // Check is vertex is close to coordinate axis
    // If true, vertex jumps on it
    public float[] isOnCoordinateAxis(float[] point) {
        float tempX = point[0];
        float tempY = point[1];
        if (Math.abs(point[0]) < 0.1 * viewScale) {
            tempX = 0;
            Log.d(TAG, "Jump on Y-Axis");
        }
        if (Math.abs(point[1]) < 0.1 * viewScale) {
            tempY = 0;
            Log.d(TAG, "Jump on X-Axis");
        }
        float newVertice[] = {tempX, tempY};
        return newVertice;
    }

    // This functions draws the coordinate system
    public void DrawCoordinateSystem() {


        glUseProgram(program);
        uColorLocation = glGetUniformLocation(program, U_COLOR);
        aPositionLocation = glGetAttribLocation(program, A_POSITION);
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);

        CoordinateSystemData.position(0);

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,

                false, 0, CoordinateSystemData);

        glEnableVertexAttribArray(aPositionLocation);

        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);

        glDrawArrays(GL_LINES, 0, 2);

        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);

        glDrawArrays(GL_LINES, 2, 2);

    }

    // This functions draws the coordinate system
    public void DrawCoordinateSystem_nonuni() {
        glUseProgram(program_nonuni);
        aColorLocation = glGetAttribLocation(program_nonuni, A_COLOR);
        aPositionLocation = glGetAttribLocation(program_nonuni, A_POSITION);
        uMatrixLocation = glGetUniformLocation(program_nonuni, U_MATRIX);

        CoordinateSystemData.position(0);

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,

                false, 0, CoordinateSystemData);

        glEnableVertexAttribArray(aPositionLocation);

        //Change later
        float black[] = {
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f, 1.0f };

        FloatBuffer tmpColorData = ByteBuffer

                .allocateDirect(black.length * 4)

                .order(ByteOrder.nativeOrder())

                .asFloatBuffer();


        tmpColorData.put(black);

        tmpColorData.position(0);

        glVertexAttribPointer(aColorLocation, 4, GL_FLOAT, false, 0, tmpColorData);

        glEnableVertexAttribArray(aColorLocation);

        glLineWidth( 2.0f );

        glDrawArrays(GL_LINES, 0, 2);

        tmpColorData.position(0);

        glVertexAttribPointer(aColorLocation, 4, GL_FLOAT, false, 0, tmpColorData);

        glEnableVertexAttribArray(aColorLocation);

        glDrawArrays(GL_LINES, 2, 2);

    }

    // This functions draws the geometries
    public void Draw(FloatBuffer vertices, int color, int numOfVertices, int startVertex, boolean filledShape) {

        glUseProgram(program);

        vertices.position(0);

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,

                false, 0, vertices);

        glEnableVertexAttribArray(aPositionLocation);

        // 0 - white, 1 - black, 2 - red, 3 - green
        if (color == 0) {
            glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        } else if (color == 1) {
            glUniform4f(uColorLocation, 0.0f, 0.0f, 0.0f, 0.0f);
        } else if (color == 2 ) {
            glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 0.0f);
        } else if (color == 3 ){
            glUniform4f(uColorLocation, 0.0f, 1.0f, 0.0f, 0.0f);
        }


        // Draw filled shapes, if filledShape is true, else only edges
        if (filledShape)
        {
            glDrawArrays(GL10.GL_TRIANGLE_FAN , startVertex, numOfVertices);
        } else
        {
            glLineWidth( 3.5f );

            glDrawArrays(GL_LINE_LOOP, startVertex, numOfVertices );
        }



    }

    // This function draws the colorbar
    public void DrawC( FloatBuffer vertices, FloatBuffer color )
    {
        glUseProgram(program_nonuni);

        aColorLocation = glGetAttribLocation(program_nonuni, A_COLOR);
        aPositionLocation = glGetAttribLocation(program_nonuni, A_POSITION);
        uMatrixLocation = glGetUniformLocation(program_nonuni, U_MATRIX);

        vertices.position(0);
        color.position(0);

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
                false, 0, vertices);

        glEnableVertexAttribArray(aPositionLocation);

        glVertexAttribPointer(aColorLocation, 4, GL_FLOAT, false, 0, color );

        glEnableVertexAttribArray(aColorLocation);

        glLineWidth( 30.0f );

        glDrawArrays(GL_LINE_STRIP, 0, 3);

        //glDrawArrays(GL_LINES, 1, 2);
    }

    // This functions draws the geometries
    public void Draw(FloatBuffer vertices, FloatBuffer color, int numOfVertices, int startVertex, boolean filledShape) {

        glUseProgram(program_nonuni);

        aColorLocation = glGetAttribLocation(program_nonuni, A_COLOR);
        aPositionLocation = glGetAttribLocation(program_nonuni, A_POSITION);
        uMatrixLocation = glGetUniformLocation(program_nonuni, U_MATRIX);

        vertices.position(0);
        color.position(0);

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,
                false, 0, vertices);

        glEnableVertexAttribArray(aPositionLocation);

        glVertexAttribPointer(aColorLocation, 4, GL_FLOAT, false, 0, color );

        glEnableVertexAttribArray(aColorLocation);


        // Draw filled shapes, if filledShape is true, else only edges
        if (filledShape)
        {
            glDrawArrays(GL_TRIANGLE_FAN, startVertex, numOfVertices);

        } else
        {
            glLineWidth( 3.5f );

            float black[] = {
                    0.0f, 0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 0.0f, 1.0f,
                    0.0f, 0.0f, 0.0f, 1.0f
            };

            FloatBuffer tmpColorData = ByteBuffer

                    .allocateDirect(black.length * 4)

                    .order(ByteOrder.nativeOrder())

                    .asFloatBuffer();

            tmpColorData.put(black);
            tmpColorData.position(0);

            glVertexAttribPointer(aColorLocation, 4, GL_FLOAT, false, 0, tmpColorData );

            glEnableVertexAttribArray(aColorLocation);

            glDrawArrays(GL_LINE_LOOP, startVertex, numOfVertices );
        }

    }

    // This function converts screen coordinates to model coordinates
    // This function is implemented twice with different arguments
    // The panning and scaling actions needs the first function, which makes sure that xOffset, yOffset and viewScale
    // won't be updated when action is performed
    // Everywhere else second function is called as it is more convenient
    public float[] getNormalizedPointPosition(View v, MotionEvent event, float xOffset, float yOffset, float viewScale ){

        final float width = (float) v.getWidth();
        final float height = (float) v.getHeight();
        float normalizedX;
        float normalizedY;

        // Differentiate between portrait and landscape
        final float aspectRatio = width > height ?
                width / height :
                height / width ;
        if ( height > width )
        {
            // Portrait
            normalizedX = ( 2f * ((event.getX() - width / 2.0f) / width)  + xOffset ) * viewScale;
            normalizedY = ( (-event.getY() + height / 2.0f) / height * 2.0f * aspectRatio + yOffset ) * viewScale;
        } else
        {
            // Landscape (is still to be implemented, scaling still wrong!!!)
            normalizedX = 2f * ((event.getX() - width / 2.0f) / width);
            normalizedY = (-event.getY() + height / 2.0f) / height * 2.0f * aspectRatio;

        }
        // Round Coordinates to two floating points
        normalizedX = Math.round( normalizedX * 100.0f ) / 100.0f;
        normalizedY = Math.round( normalizedY * 100.0f ) / 100.0f;

        float pos[]  = {normalizedX, normalizedY};
        return pos;
    }

    public float[] getNormalizedPointPosition(View v, MotionEvent event ){

        final float width = (float) v.getWidth();
        final float height = (float) v.getHeight();
        float normalizedX;
        float normalizedY;

        // Differentiate between portrait and landscape
        final float aspectRatio = width > height ?
                width / height :
                height / width ;
        if ( height > width )
        {
            // Portrait
            normalizedX = ( 2f * ((event.getX() - width / 2.0f) / width)  + getXOffset() ) * getViewScale();
            normalizedY = ( (-event.getY() + height / 2.0f) / height * 2.0f * aspectRatio + getYOffset() ) * getViewScale() ;
        } else
        {
            // Landscape (is still to be implemented, scaling still wrong!!!)
            normalizedX = 2f * ((event.getX() - width / 2.0f) / width);
            normalizedY = (-event.getY() + height / 2.0f) / height * 2.0f * aspectRatio;

        }
        // Round Coordinates to two floating points
        normalizedX = Math.round( normalizedX * 100.0f ) / 100.0f;
        normalizedY = Math.round( normalizedY * 100.0f ) / 100.0f;

        float pos[]  = {normalizedX, normalizedY};
        return pos;
    }

    // Getter functions
    public float getXOffset() {
        return xOffset;
    }

    public float getYOffset() { return yOffset;
    }

    public float getViewScale() {
        return viewScale;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public float getMaxDisplacement()
    {
        return maxDisplacement;
    }

    public float getMinDisplacement()
    {
        return minDisplacement;
    }

    public float getMinStress()
    {
        return minStress;
    }
    public float getMaxStress()
    {
        return maxStress;
    }

    public float getMinStrain()
    {
        return minStrain;
    }
    public float getMaxStrain()
    {
        return maxStrain;
    }

    public float getMinDim()
    {
        return Math.min( Math.abs(ymax - ymin), Math.abs(xmax - xmin) );
    }

    public float getMaxDim()
    {
        return Math.max( Math.abs(ymax - ymin), Math.abs(xmax - xmin) );
    }





}















