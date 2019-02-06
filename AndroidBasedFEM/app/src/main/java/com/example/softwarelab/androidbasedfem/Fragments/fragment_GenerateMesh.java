package com.example.softwarelab.androidbasedfem.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.softwarelab.androidbasedfem.MainActivity;
import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Rendering.Rectangle;
import com.example.softwarelab.androidbasedfem.Rendering.Renderer;
import com.example.softwarelab.androidbasedfem.Rendering.Triangle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class fragment_GenerateMesh extends android.support.v4.app.Fragment implements View.OnClickListener {

    private GLSurfaceView glSurfaceView;
    private Renderer renderer;
    private List<Triangle> TriangleElements = new ArrayList<Triangle>();
    private int numOfElements = 0;

    private List<Rectangle> Rectangles = new ArrayList<Rectangle>();
    private List<Triangle> Triangles = new ArrayList<Triangle>();

    private JSONObject meshedGeoJsonObj;

    private static final String TAG = "MyActivity";
    public int numOfRectangles = 0;
    public int numOfTriangles = 0;

    private EditText editTextnumElementX;
    private EditText editTextnumElementY;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View fragmentGenerateMeshView = inflater.inflate(R.layout.layout_generatemesh,container,false);

        // Create OPenGLView and add to frame
        glSurfaceView = new GLSurfaceView(this.getActivity());
        glSurfaceView.setEGLContextClientVersion(2);

        RelativeLayout frame = fragmentGenerateMeshView.findViewById(R.id.layout_generatemesh);
        frame.addView(glSurfaceView, 0);

        renderer = new Renderer(this.getActivity());
        glSurfaceView.setRenderer(renderer);

        // Read and render raw geometry instantly
        clearGeometry();
        loadRawGeometryFromJsonFile("rawGeometriesToFEMPackage.json");
        renderer.setMaxMinValues();
        renderer.centerView();


        // Some buttons and stuff
        Button btnGenerateMesh  = fragmentGenerateMeshView.findViewById(R.id.btnGeneratMesh);
        btnGenerateMesh.setOnClickListener(this);
        Button btnApproveMesh = fragmentGenerateMeshView.findViewById(R.id.btnApproveMesh);
        btnApproveMesh.setOnClickListener(this);

        editTextnumElementX = (EditText) fragmentGenerateMeshView.findViewById(R.id.editText_ElementSizeX);
        editTextnumElementY = (EditText) fragmentGenerateMeshView.findViewById(R.id.editText_ElementSizeY);


        return fragmentGenerateMeshView;
    } // end of onCreateView

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btnGeneratMesh) {
            generateMesh();
        }

        if (v.getId() == R.id.btnApproveMesh) {
            approveMesh();
        }

    } // end of onClick

    // This function finalizes mesh definition and passes data to FEMPackage
    @SuppressLint("ClickableViewAccessibility")
    public boolean generateMesh()
    {
        Integer numElementX = Integer.valueOf(editTextnumElementX.getText().toString());
        Integer numElementY = Integer.valueOf(editTextnumElementY.getText().toString());

        // CALL NATIVE LIB TO GENERATE MESH USING FEMPACKAGE
        String meshedGeoString = ((MainActivity)getActivity()).generateMesh(loadRawGeometryFromJsonFile("rawGeometriesToFEMPackage.json"), numElementX, numElementY);
        clearGeometry();

        try
        {
            meshedGeoJsonObj = new JSONObject(meshedGeoString);

            JSONObject meshedGeometry = meshedGeoJsonObj.getJSONObject("meshedGeometry");
            JSONArray meshedGeometryArray = meshedGeometry.getJSONArray("elements");

            for (int i=0; i<meshedGeometryArray.length(); i++){
                JSONObject tempGeometry = meshedGeometryArray.getJSONObject(i);
                String _type = tempGeometry.getString("type");


                if (_type.equals("triangle")){
                    Triangle tempTriangle = new Triangle();
                    TriangleElements.add( tempTriangle );

                    JSONArray vertexDataX = new JSONArray(tempGeometry.getJSONArray("vertexDataX").toString());
                    JSONArray vertexDataY = new JSONArray(tempGeometry.getJSONArray("vertexDataY").toString());
                    float[] vertices = { (float) vertexDataX.getDouble(0), (float) vertexDataY.getDouble(0),
                            (float) vertexDataX.getDouble(1), (float) vertexDataY.getDouble(1),
                            (float) vertexDataX.getDouble(2), (float) vertexDataY.getDouble(2) };
                    tempTriangle.setVertexData( vertices );

                    renderer.addTriangleToRenderer(tempTriangle, false);
                    numOfElements += 1;
                }

            }
        }
        catch (Exception e)
        {
            LogToConsole("Error in parsing meshedGeo json");
        }

        renderer.centerView();

        LogToConsole("---------------------------> Mesh is Generated <---------------------------");
        return false;
    }

    // This function finalizes mesh definition
    @SuppressLint("ClickableViewAccessibility")
    public boolean approveMesh()
    {
        saveMeshedGeometriesToJSONFile(meshedGeoJsonObj, "meshedGeometriesToFEMPackage.json");
        LogToConsole("---------------------------------------------------------------------");
        LogToConsole("Mesh is defined to be used by FEM Package");
        LogToConsole("---------------------------------------------------------------------");

        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean clearGeometry()
    {
        TriangleElements.clear();
        numOfElements = 0;
        renderer.clearFromRenderer();
        return  false;
    }

    public void LogToConsole(String str){
        NavigationView navViewConsole    = this.getActivity().findViewById(R.id.nav_view_console);
        View headerViewConsole = navViewConsole.inflateHeaderView(R.layout.header_rightnavigationmenu);
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

    public String loadRawGeometryFromJsonFile(String fileName){
        String strData = "";
        try {
//            InputStream fileIn = getActivity().getAssets().open(fileName); // to read from assets folder of app
            FileInputStream fileIn = getActivity().openFileInput(fileName);  // to read from device storage

            InputStreamReader inputStreamReader = new InputStreamReader(fileIn);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String lineData = bufferedReader.readLine();
            while (lineData != null) {
                stringBuffer.append(lineData);
                lineData = bufferedReader.readLine();
            }

            // Parse Data
            strData = stringBuffer.toString();
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

                    renderer.addTriangleToRenderer(tempTriangle, false);
                    numOfTriangles += 1;
                }

            }



            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }

        return strData;
    }

    public void saveMeshedGeometriesToJSONFile(JSONObject meshedGeoJsonObj, String fileName){
        ///// Convert Final JSON Object into a String
        String jsonData = meshedGeoJsonObj.toString();

        ///// Write String to a File
        try
        {
            FileOutputStream fileOut;
            fileOut = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOut.write(jsonData.getBytes());
            fileOut.close();
            LogToConsole("Meshed geometry is saved to file: " + fileName);
        }
        catch (Exception e)
        {
            Log.e("FileOutputStream", e.getMessage());
            LogToConsole("Exception on FileOutputStream: Couldn't write to file..");
        }

        ///// Prepare String Data to be Written into the file
//        try {
//            JSONObject finalJSONObj = new JSONObject();    // final object to be written into the file
//            JSONArray geometriesArr = new JSONArray();     // fill the array with each geometry objects
//
//            for(int i=0; i<numOfTriangles; i++){
//                float x1 = triangles.get(i).getVertices().get(0);
//                float y1 = triangles.get(i).getVertices().get(1);
//
//                float x2 = triangles.get(i).getVertices().get(2);
//                float y2 = triangles.get(i).getVertices().get(3);
//
//                float x3 = triangles.get(i).getVertices().get(4);
//                float y3 = triangles.get(i).getVertices().get(5);
//
//                JSONObject tempTriangle = new JSONObject();
//                tempTriangle.put("type", "triangle");
//
//                JSONArray VertexDataXArr = new JSONArray();
//                VertexDataXArr.put(x1);
//                VertexDataXArr.put(x2);
//                VertexDataXArr.put(x3);
//                tempTriangle.put("vertexDataX", VertexDataXArr);
//
//                JSONArray VertexDataYArr = new JSONArray();
//                VertexDataYArr.put(y1);
//                VertexDataYArr.put(y2);
//                VertexDataYArr.put(y3);
//                tempTriangle.put("vertexDataY", VertexDataYArr);
//
//                geometriesArr.put(tempTriangle);
//            }
//
//
//            finalJSONObj.put("rawGeometries", geometriesArr);  // put the array of geometry objects into final json object
//            ///// Convert Final JSON Object into a String
//            String jsonData = finalJSONObj.toString();
//
//            ///// Write String to a File
//            try
//            {
//                FileOutputStream fileOut;
//                fileOut = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
//                fileOut.write(jsonData.getBytes());
//                fileOut.close();
//                LogToConsole("Raw geometry is saved to file: " + fileName);
//            }
//            catch (Exception e)
//            {
//                Log.e("FileOutputStream", e.getMessage());
//                LogToConsole("Exception on FileOutputStream: Couldn't write to file..");
//            }
//
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

    }

}
