package com.example.berkayalpcakal.softwarelabdemo.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.berkayalpcakal.softwarelabdemo.R;
import com.example.berkayalpcakal.softwarelabdemo.Rendering.Rectangle;
import com.example.berkayalpcakal.softwarelabdemo.Rendering.Renderer;
import com.example.berkayalpcakal.softwarelabdemo.Rendering.Triangle;

import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.nio.FloatBuffer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class fragment_CreateGeometry extends android.support.v4.app.Fragment implements View.OnClickListener {
    private GLSurfaceView glSurfaceView;
    private Renderer renderer;
    private List<Rectangle> Rectangles = new ArrayList<Rectangle>();
    private List<Triangle> Triangles = new ArrayList<Triangle>();

    private static final String TAG = "MyActivity";
    public int numOfRectangles = 0;
    public int numOfTriangles = 0;
    TextView Coordinates;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View fragmentCreateGeometryView = inflater.inflate(R.layout.layout_creategeometry,container,false);

        // Create OPenGLView and add to frame
        glSurfaceView = new GLSurfaceView(this.getActivity());
        glSurfaceView.setEGLContextClientVersion(2);

        RelativeLayout frame = fragmentCreateGeometryView.findViewById(R.id.layout_creategeometry);
        frame.addView(glSurfaceView, 0);

        renderer = new Renderer(this.getActivity());
        glSurfaceView.setRenderer(renderer);

        //Define Buttons
        Button btnDrawSquare = fragmentCreateGeometryView.findViewById(R.id.btnDrawRectangle);
        btnDrawSquare.setOnClickListener(this);

        Button btnDrawTriangle = fragmentCreateGeometryView.findViewById(R.id.btnDrawTriangle);
        btnDrawTriangle.setOnClickListener(this);

        Button btnSelectEntity = fragmentCreateGeometryView.findViewById(R.id.btnSelectEntity);
        btnSelectEntity.setOnClickListener(this);

        Button btnSaveGeometry = fragmentCreateGeometryView.findViewById(R.id.btnSaveGeometry);
        btnSaveGeometry.setOnClickListener(this);

        Button btnLoadGeometry = fragmentCreateGeometryView.findViewById(R.id.btnLoadGeometry);
        btnLoadGeometry.setOnClickListener(this);

        Button btnCenterView = fragmentCreateGeometryView.findViewById(R.id.btnCenterView);
        btnCenterView.setOnClickListener(this);

        Button btnClearGeometries = fragmentCreateGeometryView.findViewById(R.id.btnClearGeometries);
        btnClearGeometries.setOnClickListener(this);

        // Write Coordinates to Screen, will be update when screen is touched
        Coordinates = fragmentCreateGeometryView.findViewById( R.id.Coordinates );
        String Text = "X: 0.00" + " Y: 0.00";
        Coordinates.setText( Text );
        Coordinates.setTextColor(Color.parseColor("#FFFFFF") );

        // Activate TouchListener
        listenToTouch();

        return fragmentCreateGeometryView;
    }

    // This function defines the Button behaviour and calls the corresponding functions
    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btnDrawRectangle)
        {
            createRectangle();
        }
        if (v.getId() == R.id.btnDrawTriangle)
        {
            createTriangle();
        }
        if (v.getId()==R.id.btnSelectEntity)
        {
            selectEntity();
        }
        if (v.getId()==R.id.btnSaveGeometry)
        {
            saveGeometry();
        }

        if (v.getId()==R.id.btnLoadGeometry)
        {
            loadGeometry();
        }
        if (v.getId()==R.id.btnCenterView)
        {
            renderer.centerView();
        }
        if (v.getId()==R.id.btnClearGeometries)
        {
            clearGeometry();
        }

        Log.e(TAG,"     End of onClick ...");

    } // end of onClick

    // This function Listens to all touch events
    // Handles scaling and panning
    // Updates screen coordinates after every touch event
    // Calls function to update coordinate system whenever screen size changes
    @SuppressLint("ClickableViewAccessibility")
    public void listenToTouch()
    {

        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {

            float[] Point;
            float[] movedPoint;
            float[] lastPoint;
            float xOffset = renderer.getXOffset();
            float yOffset = renderer.getYOffset();
            float viewScale = renderer.getViewScale();
            int a = 0;

            @Override
            public boolean onTouch( View v, MotionEvent event ) {

                renderer.updateCoordinateSystem();

                // If Pointer count is bigger than one, perform Scaling Action
                if (event.getPointerCount() > 1)
                {
                    a = 1;
                    renderer.mScaleDetector.onTouchEvent( event );

                } else // else perform panning action
                {
                    // This if statement makes sure, that we don't pan until the zooming action is finished
                    if (event.getAction() == MotionEvent.ACTION_UP && a == 1)
                    {
                        a = 0;
                    }
                    // Zooming Action will be performed if a == 0
                    if (event.getAction() == MotionEvent.ACTION_DOWN && a == 0) {
                        lastPoint = getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE && lastPoint != null && a == 0) {

                        movedPoint = getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);

                        final float dx = ( lastPoint[0] - movedPoint[0] ) / viewScale + xOffset;// ) / viewScale;
                        final float dy = ( lastPoint[1] - movedPoint[1] ) / viewScale + yOffset;// ) / viewScale;

                        renderer.setViewOffset(dx, dy);

                    }
                }

                // Update view parameters, when touch is released
                if (event.getAction() == MotionEvent.ACTION_UP ) {
                    xOffset = renderer.getXOffset();
                    yOffset = renderer.getYOffset();
                    viewScale = renderer.getViewScale();
                }

                Point = getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);
                String Text = "X: " + Point[0] + " Y: " + Point[1];
                // This two calls update the Coordinates, which are visualized on the Screen
                Coordinates.setText(Text);
                return true;
            }

        }); // End of TouchListener
    } // End of Function listenToTouch


    // This function waits for user Input, constructs a instance of class Rectangle and adds it to the Renderer
    @SuppressLint("ClickableViewAccessibility")
    public boolean createRectangle()
    {
        LogToConsole("Creating a rectangle");
        final Rectangle tempRectangle = new Rectangle();
        Rectangles.add( tempRectangle );
        LogToConsole("...... tap two points to draw rectangle");

        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            boolean firstPointDone = false;
            boolean secondPointDone = false;
            float[] posFirstPoint  = {0f, 0f};
            float[] posSecondPoint = {0f, 0f};
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(firstPointDone == false){
                    posFirstPoint = renderer.IntersectionTest( getNormalizedPointPosition(v, event ) );
                    firstPointDone = true;
                }
                else if(firstPointDone == true && secondPointDone == false){
                    posSecondPoint = renderer.IntersectionTest( getNormalizedPointPosition(v, event) );
                    secondPointDone = true;

                    float[] vertices = { posFirstPoint[0], posFirstPoint[1], posSecondPoint[0], posSecondPoint[1] };
                    tempRectangle.setVertexData( vertices );
                    LogToConsole("............ shape initialized with width: " +
                            Math.abs(posSecondPoint[0] - posFirstPoint[0]) + ",  height: " + Math.abs(posSecondPoint[1] - posFirstPoint[1]));

                    popupUpdateRectangleShape(v, tempRectangle, posFirstPoint, posSecondPoint);
                    renderer.addRectangleToRenderer(tempRectangle);
                    numOfRectangles += 1;
                    listenToTouch();
                    return true;
                }
                return false;
            } // end of onTouch
        }); // End of onTouchListener

        Log.e(TAG,"     End of onTouchListener ...");
        return false;

    } // End of createRectangle()

    // This function waits for user Input, constructs a instance of class Triangle and adds it to the Renderer
    @SuppressLint("ClickableViewAccessibility")
    public boolean createTriangle()
    {
        final Triangle tempTriangle = new Triangle();
        LogToConsole("Creating a triangle");
        LogToConsole("...... tap three points to draw triangle");
        Triangles.add( tempTriangle );

        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            boolean firstPointDone = false;
            boolean secondPointDone = false;
            boolean thirdPointDone = false;
            float[] posFirstPoint = {0f, 0f};
            float[] posSecondPoint = {0f, 0f};
            float[] posThirdPoint = {0f, 0f};

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (firstPointDone == false) {
//                             Log.d(TAG, "Ready to get first point...");
                    posFirstPoint = renderer.IntersectionTest( getNormalizedPointPosition(v, event) );
//                             Log.d(TAG, "        First point with : " + posFirstPoint[0] + " " + posFirstPoint[1]);
                    firstPointDone = true;
                } else if (firstPointDone == true && secondPointDone == false) {
//                             Log.d(TAG, "Ready to get second point...");
                    posSecondPoint = renderer.IntersectionTest( getNormalizedPointPosition(v, event) );
//                             Log.d(TAG, "        Second point with : " + posSecondPoint[0] + " " + posSecondPoint[1]);
                    secondPointDone = true;
//                             Log.d(TAG, "Now we have two points. Ready to create rectangle!!!");
                } else if (firstPointDone == true && secondPointDone == true && thirdPointDone == false) {
//                             Log.d(TAG, "Ready to get third point...");
                    posThirdPoint = renderer.IntersectionTest( getNormalizedPointPosition(v, event) );
//                             Log.d(TAG, "        Third point with : " + posSecondPoint[0] + " " + posSecondPoint[1]);
                    thirdPointDone = true;
//                             Log.d(TAG, "Now we have three points. Ready to create Triangle!!!");
                    float[] vertices = { posFirstPoint[0], posFirstPoint[1], posSecondPoint[0], posSecondPoint[1], posThirdPoint[0], posThirdPoint[1] };
                    tempTriangle.setVertexData( vertices );
                    renderer.addTriangleToRenderer(tempTriangle);
                    numOfTriangles += 1;
                    LogToConsole("...... triangle is created");
                    listenToTouch();
                    return true;
                }
                return false;

            } // end of onTouch
        });// End of onTouchListener

        Log.e(TAG,"     End of onTouchListener ...");
        return false;

    } // End of createTriangle()


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
            normalizedX = ( 2f * ((event.getX() - width / 2.0f) / width)  + renderer.getXOffset() ) * renderer.getViewScale();
            normalizedY = ( (-event.getY() + height / 2.0f) / height * 2.0f * aspectRatio + renderer.getYOffset() ) * renderer.getViewScale() ;
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

    // This function calls the insideOutsideTest function to select geometries
    @SuppressLint("ClickableViewAccessibility")
    public boolean selectEntity()
    {
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            float[] Point;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Point = getNormalizedPointPosition(v, event );
                int[] res = renderer.insideOutsideTest( Point ); // res[type, index]
                if (res[0] >= 0)    // if geometry is selected
                    popupDeleteSelectedGeometry(getView(),res);

                // Enable TouchListener again
                listenToTouch();
                return true;
            } // end of onTouch
        }); // End of onTouchListener

        return true;

    } // End of selectEntity

    // Calls function to write Geometry data to json- file
    @SuppressLint("ClickableViewAccessibility")
    public boolean saveGeometry()
    {
        popupSaveGeometry(getView(), Rectangles, Triangles);
        return false;
    }

    // Calls function to read Geometry data from json- file
    @SuppressLint("ClickableViewAccessibility")
    public boolean loadGeometry()
    {
        clearGeometry();
        popupLoadGeometry(getView());
        return  false;
    }

    // Calls function to read Geometry data from json- file
    @SuppressLint("ClickableViewAccessibility")
    public boolean clearGeometry()
    {
        Rectangles.clear();
        Triangles.clear();
        numOfRectangles = 0;
        numOfTriangles = 0;
        renderer.clearFromRenderer();
        return  false;
    }

    public void popupUpdateRectangleShape(View v, final Rectangle rectangle, final float[] first, final float[] second){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_creategeometry_rectangle,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final EditText editText_width  = (EditText) promptsView.findViewById(R.id.editText_width);
        final EditText editText_height = (EditText) promptsView.findViewById(R.id.editText_height);

        alertBuilder.setCancelable(true)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // avoid crushing app in case of empty entries
                        if( !editText_width.getText().toString().matches("") ) {
                            if ( !editText_height.getText().toString().matches("") ){


                                float width = Float.valueOf(editText_width.getText().toString());
                                float height = Float.valueOf(editText_height.getText().toString());

                                // if entries are valid, update coordinates
                                if (width > 0 || height > 0) {
                                    FloatBuffer vertexData = rectangle.getVertices();
                                    float dx = width;
                                    float dy = height;
                                    float x2 = 0, y2 = 0;

                                    if (second[0] - first[0] > 0)
                                        x2 = first[0] + dx;
                                    else
                                        x2 = first[0] - dx;

                                    if (second[1] - first[1] > 0)
                                        y2 = first[1] + dy;
                                    else
                                        y2 = first[1] - dy;

                                    float[] vertices = {first[0], first[1], x2, y2};
                                    rectangle.setVertexData(vertices);
                                    renderer.centerView();
                                    LogToConsole("............ shape updated with width: " + dx + ",   height: " + dy);
                                }
                            } else
                                LogToConsole("............Enter valid weight and height values. Updating shape was cancelled");

                        }
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public void popupSaveGeometry(View v, List<Rectangle> rectangles, List<Triangle> triangles){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_savegeometry,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final EditText editText_saveGeometry  = (EditText) promptsView.findViewById(R.id.editText_saveGeometry);

        alertBuilder.setCancelable(true)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // avoid crushing app in case of empty entries
                        if( !editText_saveGeometry.getText().toString().matches("") ) {
                            saveGeometryToJSONFile(Rectangles, Triangles, editText_saveGeometry.getText().toString());
                            LogToConsole("---------------------------> Geometry Saved <---------------------------");
                        }
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public void popupLoadGeometry(View v){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_loadgeometry,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final EditText editText_loadGeometry  = (EditText) promptsView.findViewById(R.id.editText_loadGeometry);

        alertBuilder.setCancelable(true)
                .setPositiveButton("Load", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // avoid crushing app in case of empty entries
                        if( !editText_loadGeometry.getText().toString().matches("") ) {
                            loadGeometryFromJsonFile(editText_loadGeometry.getText().toString());
                            renderer.centerView();
                            LogToConsole("---------------------------> Geometry Loaded <---------------------------");
                        }
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public void popupDeleteSelectedGeometry(View v, final int[] arr){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_deleteselectedgeometry,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        alertBuilder.setCancelable(false)
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (arr[0] == 0)
                            Rectangles.get(arr[1]).changeColor(0);
                        if (arr[0] == 1)
                            Triangles.get(arr[1]).changeColor(0);
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (arr[0] == 0) {
                            Rectangles.remove(arr[1]);
                            renderer.removeRectanglesFromRenderer(arr[1]);
                            numOfRectangles--;
                        }
                        if (arr[0] == 1) {
                            Triangles.remove(arr[1]);
                            renderer.removeTriangleFromRenderer(arr[1]);
                            numOfTriangles--;
                        }
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public void LogToConsole(String str){
        NavigationView navViewConsole    = this.getActivity().findViewById(R.id.nav_view_console);
        View           headerViewConsole = navViewConsole.inflateHeaderView(R.layout.header_rightnavigationmenu);
        LinearLayout consoleLayout     = (LinearLayout) headerViewConsole.findViewById(R.id.layout_console);

        TextView tv = new TextView(this.getActivity());
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.LEFT;
        params.setMargins(10, 10, 10, 10);
        tv.setLayoutParams(params);
        tv.setText(str);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        tv.setBackgroundColor( 0x300a24);
        consoleLayout.addView(tv);
    }

    public void saveGeometryToJSONFile(List<Rectangle> rectangles, List<Triangle> triangles, String fileName){
        ///// Prepare String Data to be Written into the file
        try {
            JSONObject finalJSONObj = new JSONObject();    // final object to be written into the file
            JSONArray  geometriesArr = new JSONArray();     // fill the array with each geometry objects

            for(int i=0; i<numOfRectangles; i++){
                float x1 = rectangles.get(i).getVertices().get(0);
                float y1 = rectangles.get(i).getVertices().get(1);

                float x2 = rectangles.get(i).getVertices().get(4);
                float y2 = rectangles.get(i).getVertices().get(5);

                JSONObject tempRectangleJSON = new JSONObject();
                tempRectangleJSON.put("type", "rectangle");

                JSONArray VertexDataXArr = new JSONArray();
                VertexDataXArr.put(x1);
                VertexDataXArr.put(x2);
                tempRectangleJSON.put("vertexDataX", VertexDataXArr);

                JSONArray VertexDataYArr = new JSONArray();
                VertexDataYArr.put(y1);
                VertexDataYArr.put(y2);
                tempRectangleJSON.put("vertexDataY", VertexDataYArr);

                geometriesArr.put(tempRectangleJSON);
            }

            for(int i=0; i<numOfTriangles; i++){
                float x1 = triangles.get(i).getVertices().get(0);
                float y1 = triangles.get(i).getVertices().get(1);

                float x2 = triangles.get(i).getVertices().get(2);
                float y2 = triangles.get(i).getVertices().get(3);

                float x3 = triangles.get(i).getVertices().get(4);
                float y3 = triangles.get(i).getVertices().get(5);

                JSONObject tempTriangle = new JSONObject();
                tempTriangle.put("type", "triangle");

                JSONArray VertexDataXArr = new JSONArray();
                VertexDataXArr.put(x1);
                VertexDataXArr.put(x2);
                VertexDataXArr.put(x3);
                tempTriangle.put("vertexDataX", VertexDataXArr);

                JSONArray VertexDataYArr = new JSONArray();
                VertexDataYArr.put(y1);
                VertexDataYArr.put(y2);
                VertexDataYArr.put(y3);
                tempTriangle.put("vertexDataY", VertexDataYArr);

                geometriesArr.put(tempTriangle);
            }


            finalJSONObj.put("rawGeometries", geometriesArr);  // put the array of geometry objects into final json object
            ///// Convert Final JSON Object into a String
            String jsonData = finalJSONObj.toString();

            ///// Write String to a File
            try
            {
                FileOutputStream fileOut;
                fileOut = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
                fileOut.write(jsonData.getBytes());
                fileOut.close();
                LogToConsole("Raw geometry is saved to file: " + fileName);
            }
            catch (Exception e)
            {
                Log.e("FileOutputStream", e.getMessage());
                LogToConsole("Exception on FileOutputStream: Couldn't write to file..");
            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void loadGeometryFromJsonFile(String fileName){
        try {
            FileInputStream fileIn = getActivity().openFileInput(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(fileIn);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String lineData = bufferedReader.readLine();
            while (lineData != null) {
                stringBuffer.append(lineData);
                lineData = bufferedReader.readLine();
            }

            // Parse Data
            String strData = stringBuffer.toString();
            JSONObject obj = new JSONObject(strData);
            String geometriesData = obj.getJSONArray("rawGeometries").toString();

            JSONArray geometries = new JSONArray(geometriesData);
            for (int i=0; i<geometries.length(); i++){
                JSONObject tempGeometry = geometries.getJSONObject(i);
                String _type = tempGeometry.getString("type");

                if (_type.equals("rectangle")){
                    Rectangle tempRectangle = new Rectangle();
                    Rectangles.add( tempRectangle );

                    JSONArray vertexDataX = new JSONArray(tempGeometry.getJSONArray("vertexDataX").toString());
                    JSONArray vertexDataY = new JSONArray(tempGeometry.getJSONArray("vertexDataY").toString());
                    float[] vertices = { (float) vertexDataX.getDouble(0), (float) vertexDataY.getDouble(0),
                            (float) vertexDataX.getDouble(1), (float) vertexDataY.getDouble(1) };
                    tempRectangle.setVertexData( vertices );

                    renderer.addRectangleToRenderer(tempRectangle);
                    numOfRectangles += 1;
                }

                if (_type.equals("triangle")){
                    Triangle tempTriangle = new Triangle();
                    Triangles.add( tempTriangle );

                    JSONArray vertexDataX = new JSONArray(tempGeometry.getJSONArray("vertexDataX").toString());
                    JSONArray vertexDataY = new JSONArray(tempGeometry.getJSONArray("vertexDataY").toString());
                    float[] vertices = { (float) vertexDataX.getDouble(0), (float) vertexDataY.getDouble(0),
                            (float) vertexDataX.getDouble(1), (float) vertexDataY.getDouble(1),
                            (float) vertexDataX.getDouble(2), (float) vertexDataY.getDouble(2) };
                    tempTriangle.setVertexData( vertices );

                    renderer.addTriangleToRenderer(tempTriangle);
                    numOfTriangles += 1;
                }

            }

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
    }

} // end of fragment class
