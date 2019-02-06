package com.example.softwarelab.androidbasedfem;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.softwarelab.androidbasedfem.Fragments.fragment_BoundaryConditions;
import com.example.softwarelab.androidbasedfem.Fragments.fragment_CreateGeometry;
import com.example.softwarelab.androidbasedfem.Fragments.fragment_DefineMaterial;
import com.example.softwarelab.androidbasedfem.Fragments.fragment_GenerateMesh;
import com.example.softwarelab.androidbasedfem.Fragments.fragment_Results;
import com.example.softwarelab.androidbasedfem.Fragments.fragment_ResultsModal;
import com.example.softwarelab.androidbasedfem.Fragments.fragment_Solve;
import com.example.softwarelab.androidbasedfem.Fragments.fragment_StartingPage;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle topLeftToggle;

//    private GLSurfaceView glSurfaceView;
//    private boolean rendererSet = false;
//    private Renderer renderer;
//    private List<Rectangle> Rectangles = new ArrayList<Rectangle>();

    static
    {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // set layout

        // Configure Toggle Button on Top-Left
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        topLeftToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(topLeftToggle);
        topLeftToggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configure Navigation Menu on Left
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

//        // Configure OPENGL
//        glSurfaceView = new GLSurfaceView(this);
//        glSurfaceView.setEGLContextClientVersion(2);  // Request an OpenGL ES 2.0 compatible context.

        getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_StartingPage()).commit();
    }


    /////////////// Take Action when Toggle Button is Pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(topLeftToggle.onOptionsItemSelected(item))
            return true;
        return super.onOptionsItemSelected(item);
    }


    /////////////// Take Action when Menu Item is Selected
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.menu_creategeometry:
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_CreateGeometry()).commit();
                break;

            case R.id.menu_definematerial:
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_DefineMaterial()).commit();
                break;

            case R.id.menu_generatemesh:
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_GenerateMesh()).commit();
                break;

            case R.id.menu_boundaryconditions:
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_BoundaryConditions()).commit();
                break;

            case R.id.menu_solve:
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_Solve()).commit();
                break;

            case R.id.menu_results:
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_Results()).commit();
                break;

            case R.id.menu_resultsModal:
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_ResultsModal()).commit();
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native String generateMesh(String str, int sizeX, int sizeY);

    public native String solveStaticSystem(String meshedGeo, String material, String bc);

    public native String solveDynamicSystem(String meshedGeo, String material, String bc, String modalSettings);

    public native String postProcess(String meshedGeo, String material, String strDisp);
}
