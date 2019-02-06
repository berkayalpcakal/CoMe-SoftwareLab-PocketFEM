package com.example.berkayalpcakal.softwarelabdemo;

import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.berkayalpcakal.softwarelabdemo.Fragments.fragment_CreateGeometry;
import com.example.berkayalpcakal.softwarelabdemo.Fragments.fragment_DefineMaterial;
import com.example.berkayalpcakal.softwarelabdemo.Fragments.fragment_GenerateMesh;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle topLeftToggle;

//    private GLSurfaceView glSurfaceView;
//    private boolean rendererSet = false;
//    private Renderer renderer;
//    private List<Rectangle> Rectangles = new ArrayList<Rectangle>();

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
//                Toast.makeText(this, "Create Geometry", Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_CreateGeometry()).commit();
                break;

            case R.id.menu_definematerial:
//                Toast.makeText(this, "Define Material", Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_DefineMaterial()).commit();
                break;

            case R.id.menu_generatemesh:
//                Toast.makeText(this, "Generate Mesh", Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_GenerateMesh()).commit();
                break;

            case R.id.menu_boundaryconditions:
//                Toast.makeText(this, "Boundary Conditions", Toast.LENGTH_SHORT).show();
//                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_GenerateMesh()).commit();
                break;

            case R.id.menu_applyload:
//                Toast.makeText(this, "Apply Load", Toast.LENGTH_SHORT).show();
//                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_GenerateMesh()).commit();
                break;

            case R.id.menu_solve:
//                Toast.makeText(this, "Solve", Toast.LENGTH_SHORT).show();
//                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_GenerateMesh()).commit();
                break;

            case R.id.menu_results:
//                Toast.makeText(this, "Results", Toast.LENGTH_SHORT).show();
//                getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new fragment_GenerateMesh()).commit();
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
}
