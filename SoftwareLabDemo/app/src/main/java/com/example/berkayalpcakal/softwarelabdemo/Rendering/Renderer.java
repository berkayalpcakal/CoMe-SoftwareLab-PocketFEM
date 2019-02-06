package com.example.berkayalpcakal.softwarelabdemo.Rendering;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_LINES;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.glClear;

import static android.opengl.GLES20.glClearColor;

import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glDrawElements;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.orthoM;
import static android.support.constraint.Constraints.TAG;
import static javax.microedition.khronos.opengles.GL10.GL_FLOAT;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.ScaleGestureDetector;

import com.example.berkayalpcakal.softwarelabdemo.Utils.LoggerConfig;
import com.example.berkayalpcakal.softwarelabdemo.R;
import com.example.berkayalpcakal.softwarelabdemo.Utils.TextResourceReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;


public class Renderer implements GLSurfaceView.Renderer {
    private static final int BYTES_PER_FLOAT = 4;
    private static final int POSITION_COMPONENT_COUNT = 2;
    private final Context context;
    private int program;

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

    // Shader information ( Check: Kevin Brothaler, OpenGL ES 2 for Android )
    private static final String U_MATRIX = "u_Matrix";
    private int uMatrixLocation;
    private static final String U_COLOR = "u_Color";
    private int uColorLocation;
    private static final String A_POSITION = "a_Position";
    private int aPositionLocation;

    // View Parameters
    private float aspectRatio;
    private float viewScale = 1.0f;
    private float xOffset;
    private float yOffset;

    // ScaleGestureDetector to handle scaling
    public ScaleGestureDetector mScaleDetector;

    // Constructor
    // Allocates standard coordinate system
    // Allocates ScaleGestureDetector
    public Renderer(Context context) {
        this.context = context;

        float[] tableVertices = {

                // x_Axis

                -1.0f, 0.0f,

                1.0f, 0.0f,

                // y-Axis

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

        String vertexShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.vertex_shader);

        String fragmentShaderSource = TextResourceReader
                .readTextFileFromResource(context, R.raw.fragment_shader);

        // Compile Shaders
        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper.compileFragmentShader(fragmentShaderSource);
        // Link Shaders to program
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

        DrawCoordinateSystem();

        // Loop over all triangles
        int lengthTri = Triangles.size();
        for (int i = 0; i < lengthTri; i++) {
            Triangles.get(i).Draw(this);
        }

        // Loop over all geometries
        int lengthRec = Rectangles.size();
        for (int i = 0; i < lengthRec; i++) {
            Rectangles.get(i).Draw(this);
        }

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

    // These functions add the new geometries to the pointer list
    // After that geometries will directly appear on the screen, as onDrawFrame will call the corresponding drawing function
    public void addTriangleToRenderer(Triangle triangle) {
        Triangles.add(triangle);
    }

    public void addRectangleToRenderer(Rectangle rectangle) {
        Rectangles.add(rectangle);
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
        int lengthRec = Rectangles.size();
        int lengthTri = Triangles.size();
        FloatBuffer verticesRec;
        FloatBuffer verticesTri;
        // Look for better option
        float xmin = 10000000.0f;
        float xmax = -10000000.0f;
        float ymin = 10000000.0f;
        float ymax = -10000000.0f;

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
            }
        }

        if ((ymax - ymin) > aspectRatio * (xmax - xmin)) {
            viewScale = (ymax - ymin);
        } else {
            viewScale = (xmax - xmin);
        }

        if (lengthRec > 0 || lengthTri > 0) {
            xOffset = ((xmax + xmin) / 2.0f) / viewScale;
            yOffset = ((ymax + ymin) / 2.0f) / viewScale;
        }
        updateCoordinateSystem();
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
                Rectangles.get(i).changeColor(1);
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
        CoordinateSystemData.position(0);

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,

                false, 0, CoordinateSystemData);

        glEnableVertexAttribArray(aPositionLocation);

        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);

        glDrawArrays(GL_LINES, 0, 2);

        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);

        glDrawArrays(GL_LINES, 2, 2);

    }

    // This functions draws the geometries
    public void Draw(FloatBuffer vertices, int color, int numOfVertices) {
        vertices.position(0);

        glVertexAttribPointer(aPositionLocation, POSITION_COMPONENT_COUNT, GL_FLOAT,

                false, 0, vertices);

        glEnableVertexAttribArray(aPositionLocation);

        // Draw filled shapes
        if (color == 0) {
            glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f);
        } else if (color == 1) {
            glUniform4f(uColorLocation, 0.0f, 1.0f, 0.0f, 0.0f);
        }

        glDrawArrays(GL_TRIANGLE_FAN, 0, numOfVertices);

        // Draw edges of shapes
        glUniform4f(uColorLocation, 0.0f, 0.0f, 0.0f, 0.0f );

        glLineWidth( 3.5f );

        glDrawArrays(GL_LINE_LOOP, 0, numOfVertices );
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



}















