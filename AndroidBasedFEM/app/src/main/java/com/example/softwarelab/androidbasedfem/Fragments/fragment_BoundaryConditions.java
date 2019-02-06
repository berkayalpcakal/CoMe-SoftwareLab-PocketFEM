package com.example.softwarelab.androidbasedfem.Fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Looper;
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

import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Rendering.BoundaryCondition;
import com.example.softwarelab.androidbasedfem.Rendering.ForceArrow;
import com.example.softwarelab.androidbasedfem.Rendering.Rectangle;
import com.example.softwarelab.androidbasedfem.Rendering.Renderer;
import com.example.softwarelab.androidbasedfem.Rendering.RollerSupport;
import com.example.softwarelab.androidbasedfem.Rendering.Triangle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class fragment_BoundaryConditions extends android.support.v4.app.Fragment implements View.OnClickListener {

    private GLSurfaceView glSurfaceView;
    private Renderer renderer;

    private List<Triangle> TriangleElements = new ArrayList<Triangle>();
    private List<BoundaryCondition> BoundaryElements = new ArrayList<BoundaryCondition>();

    private List<Integer> DofsToFix = new ArrayList<Integer>();

    private List<Integer> DofsForceVector = new ArrayList<Integer>();
    private List<Float> ValueForce = new ArrayList<Float>();

    private String meshedGeometryJsonString;
    private int numOfDof;

    float tmpForceValue;
    float[] tmpPoint = {0f, 0f};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentBoundaryConditionsView = inflater.inflate(R.layout.layout_boundaryconditions, container, false);

        // Create OPenGLView and add to frame
        glSurfaceView = new GLSurfaceView(this.getActivity());
        glSurfaceView.setEGLContextClientVersion(2);

        RelativeLayout frame = fragmentBoundaryConditionsView.findViewById(R.id.layout_boundaryconditions);
        frame.addView(glSurfaceView, 0);

        renderer = new Renderer(this.getActivity());
        glSurfaceView.setRenderer(renderer);

        // Read and render mesh instantly
        clearGeometry();
        loadMeshedGeometryFromJsonFile("meshedGeometriesToFEMPackage.json");
        renderer.setMaxMinValues();
        renderer.centerView();

        // Some buttons
        Button btnApproveBoundaryConditions = (Button) fragmentBoundaryConditionsView.findViewById(R.id.btnApproveBoundaryConditions);
        btnApproveBoundaryConditions.setOnClickListener(this);
        Button btnAddConstraintX = (Button) fragmentBoundaryConditionsView.findViewById(R.id.btnAddConstraintX);
        btnAddConstraintX.setOnClickListener(this);
        Button btnAddConstraintY = (Button) fragmentBoundaryConditionsView.findViewById(R.id.btnAddConstraintY);
        btnAddConstraintY.setOnClickListener(this);
        Button btnAddForceY = (Button) fragmentBoundaryConditionsView.findViewById(R.id.btnAddForceY);
        btnAddForceY.setOnClickListener(this);
        Button btnAddForceX = (Button) fragmentBoundaryConditionsView.findViewById(R.id.btnAddForceX);
        btnAddForceX.setOnClickListener(this);
        Button btnCenterViewBC = (Button) fragmentBoundaryConditionsView.findViewById(R.id.btnCenterViewBC);
        btnCenterViewBC.setOnClickListener(this);

        listenToTouch();

        return fragmentBoundaryConditionsView;
    } // end of onCreateView

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btnApproveBoundaryConditions) {
            approveBoundaryConditions();
        }

        if (v.getId() == R.id.btnCenterViewBC) {
            renderer.centerView();

        }

        if (v.getId() == R.id.btnAddConstraintX) {
            applyBoundaryCondition(0);
        }

        if (v.getId() == R.id.btnAddConstraintY) {
            applyBoundaryCondition(1);

        }

        if (v.getId() == R.id.btnAddForceX)
        {
            applyLoad(0);
        }

        if (v.getId() == R.id.btnAddForceY)
        {
            applyLoad(1);
        }
    } // end of onClick


    // This function finalizes boundary condition definition
    @SuppressLint("ClickableViewAccessibility")
    public boolean approveBoundaryConditions() {
        saveBCToJSONFile();
        LogToConsole("---------------------------------------------------------------------");
        LogToConsole("Boundary conditions are defined to be used by FEM Package");
        LogToConsole("---------------------------------------------------------------------");
        return false;
    }

//    private List<Integer> DofsToFix = new ArrayList<Integer>();
//    private List<Integer> DofsForceVector = new ArrayList<Integer>();
//    private List<Float> ValueForce = new ArrayList<Float>();
    public void saveBCToJSONFile(){
        ///// Prepare String Data to be Written into the file
        try {
            JSONObject finalJSONObj = new JSONObject();    // final object to be written into the file
            JSONObject BCObj = new JSONObject();

            JSONArray fixObj = new JSONArray();
            for (int i=0; i<DofsToFix.size(); i++)
            {
                fixObj.put(DofsToFix.get(i));
            }
            BCObj.put("Fix", fixObj);


            JSONObject forceObj = new JSONObject();
            JSONArray forceDofsObj = new JSONArray();
            JSONArray forceValuesObj = new JSONArray();

            for (int i=0; i<DofsForceVector.size(); i++)
            {
                forceDofsObj.put(DofsForceVector.get(i));
                forceValuesObj.put(ValueForce.get(i));
            }
            forceObj.put("DoFs", forceDofsObj);
            forceObj.put("Values", forceValuesObj);
            BCObj.put("Force", forceObj);


            finalJSONObj.put("BC", BCObj);  // put the material object into final json object

            ///// Convert Final JSON Object into a String
            String jsonData = finalJSONObj.toString();

            ///// Write String to a File
            try
            {
                FileOutputStream fileOut;
                fileOut = getActivity().openFileOutput("BCToFEMPackage.json", Context.MODE_PRIVATE);
                fileOut.write(jsonData.getBytes());
                fileOut.close();
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


    @SuppressLint("ClickableViewAccessibility")
    public boolean clearGeometry() {
        TriangleElements.clear();
        renderer.clearFromRenderer();
        return false;
    }

    public void LogToConsole(String str) {
        NavigationView navViewConsole = this.getActivity().findViewById(R.id.nav_view_console);
        View headerViewConsole = navViewConsole.inflateHeaderView(R.layout.header_rightnavigationmenu);
        LinearLayout consoleLayout = (LinearLayout) headerViewConsole.findViewById(R.id.layout_console);

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
        tv.setBackgroundColor(0x300a24);
        consoleLayout.addView(tv);
    }

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
            public boolean onTouch(View v, MotionEvent event ) {

                renderer.updateCoordinateSystem();
                int BoundarySize = BoundaryElements.size();
                for( int i = 0; i < BoundarySize; i++)
                {
                    BoundaryElements.get(i).updateVertexData( renderer.getViewScale(), renderer.getMinDim() );
                }

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
                        lastPoint = renderer.getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE && lastPoint != null && a == 0) {

                        movedPoint = renderer.getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);

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

                Point = renderer.getNormalizedPointPosition(v, event, xOffset, yOffset, viewScale);
                String Text = "X: " + Point[0] + " Y: " + Point[1];
                // This two calls update the Coordinates, which are visualized on the Screen
                return true;
            }

        }); // End of TouchListener
    } // End of Function listenToTouch

    @SuppressLint("ClickableViewAccessibility")
    public boolean applyLoad(final int direction )
    {
        final ForceArrow tmpForce = new ForceArrow();
        LogToConsole("Applying Load");
        BoundaryElements.add( tmpForce );


        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {


            boolean forceSetUp = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                tmpPoint = renderer.IntersectionTest( renderer.getNormalizedPointPosition(v, event) );
                tmpForce.setVertexData( tmpPoint, direction, renderer.getViewScale(), renderer.getMinDim() );
                renderer.addBoundaryConditionToRenderer( tmpForce );
                popupApplyLoad(v, direction);

                LogToConsole("...... Load is applied");
                listenToTouch();
                return false;
            } // end of onTouch
        });// End of onTouchListener

        Log.e(TAG,"     End of onTouchListener ...");

        return false;
    } // End of applyLoad()


    @SuppressLint("ClickableViewAccessibility")
    public boolean popupApplyLoad(View v , final int direction){
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.popup_applyload,null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
        alertBuilder.setView(promptsView);

        final EditText editText_ForceValue  = (EditText) promptsView.findViewById(R.id.editText_saveGeometry);


        alertBuilder.setCancelable(true)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        tmpForceValue = Float.valueOf(editText_ForceValue.getText().toString());
                        // Assume ForceY to e positive downwards
                        if(direction==1)
                        {
                            tmpForceValue *= (-1);
                        }

                        searchForDofandAddtoForceVector(tmpPoint, direction, tmpForceValue);

                        // avoid crushing app in case of empty entries
                        //if( !editText_saveGeometry.getText().toString().matches("") ) {
                        //    saveGeometryToJSONFile(Rectangles, Triangles, editText_saveGeometry.getText().toString());
                        LogToConsole("---------------------------> Load applied <---------------------------");
                    }



                });
        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();

        return false;
    }

    // This Function finds the Dofs corresponding to a node and sets up the force vector
    // Direction has to be zero for x and 1 for y direction
    public void searchForDofandAddtoForceVector( float[] point, int direction, float value )
    {
        int lengthTri = TriangleElements.size();
        int tmp;
        float x;
        float y;

        for (int i = 0; i < lengthTri; i++) {

            FloatBuffer Vertices = TriangleElements.get(i).getVertices();

            for( int j = 0; j < 6; j = j + 2)
            {
                x = Vertices.get(j);
                y = Vertices.get(j+1);

                if( (point[0] == x) && (point[1] == y) )
                {
                    tmp = TriangleElements.get(i).getDof( j + direction );
                    DofsForceVector.add( tmp );
                    ValueForce.add( value );
                    return;
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean applyBoundaryCondition( final int direction )
    {
        final RollerSupport tmpRoller = new RollerSupport();
        LogToConsole("Applying Load");
        BoundaryElements.add( tmpRoller );


        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if( event.getAction() == MotionEvent.ACTION_DOWN ) {
                    tmpPoint = renderer.IntersectionTest(renderer.getNormalizedPointPosition(v, event));
                    tmpRoller.setVertexData(tmpPoint, direction, renderer.getViewScale(), renderer.getMinDim() );
                    renderer.addBoundaryConditionToRenderer(tmpRoller);
                    searchForDofandAddtoList( tmpPoint, direction );

                }

                LogToConsole("...... Load is applied");
                listenToTouch();
                return false;
            } // end of onTouch
        });// End of onTouchListener

        Log.e(TAG,"     End of onTouchListener ...");

        return false;
    } // End of applyLoad()

    // This Function finds the Dofs corresponding to a node
    // Direction has to be zero for x and 1 for y direction
    public void searchForDofandAddtoList( float[] point, int direction )
    {
        int lengthTri = TriangleElements.size();
        int tmp;
        float x;
        float y;

        for (int i = 0; i < lengthTri; i++) {

            FloatBuffer Vertices = TriangleElements.get(i).getVertices();

            for( int j = 0; j < 6; j = j + 2)
            {
                x = Vertices.get(j);
                y = Vertices.get(j+1);

                if( (point[0] == x) && (point[1] == y) )
                {
                    tmp = TriangleElements.get(i).getDof( j + direction );
                    DofsToFix.add( tmp );
                    return;
                }
            }
        }
    } // End of searchForDofandAddtoList


    // This function reads the Json files and stores the information in a List of Triangles
    public String loadMeshedGeometryFromJsonFile(String fileName){
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
            String strData = stringBuffer.toString();
            meshedGeometryJsonString = strData;

            JSONObject obj = new JSONObject(strData);
            JSONObject meshedGeometryObj = obj.getJSONObject("meshedGeometry");

            numOfDof = meshedGeometryObj.getInt("numOfDof");
            JSONArray elementsArray = meshedGeometryObj.getJSONArray("elements");

            for (int i=0; i<elementsArray.length(); i++){
                JSONObject tempElement = elementsArray.getJSONObject(i);
                String _type = tempElement.getString("type");
                JSONArray eftarrobj = tempElement.getJSONArray("EFT");

                if (_type.equals("triangle")){
                    Triangle tempTriangle = new Triangle();
                    TriangleElements.add( tempTriangle );

                    JSONArray vertexDataX = new JSONArray(tempElement.getJSONArray("vertexDataX").toString());
                    JSONArray vertexDataY = new JSONArray(tempElement.getJSONArray("vertexDataY").toString());
                    float[] vertices = { (float) vertexDataX.getDouble(0), (float) vertexDataY.getDouble(0),
                            (float) vertexDataX.getDouble(1), (float) vertexDataY.getDouble(1),
                            (float) vertexDataX.getDouble(2), (float) vertexDataY.getDouble(2) };
                    tempTriangle.setVertexData( vertices );

                    int[] dofs = { (int) eftarrobj.getInt(0), (int) eftarrobj.getInt(1), (int) eftarrobj.getInt(2),
                            (int) eftarrobj.getInt(3), (int) eftarrobj.getInt(4), (int) eftarrobj.getInt(5) };

                    tempTriangle.setEFT( dofs );

                    renderer.addTriangleToRenderer(tempTriangle, false);
                }

            }
            renderer.centerView();

            // End of Parse Data
        }catch (Exception e){
            Log.e("FileInputStream", e.getMessage());
            LogToConsole("Exception on FileInputStream: Couldn't read from file..");
        }
        return meshedGeometryJsonString;
    }
} // End of fragment class