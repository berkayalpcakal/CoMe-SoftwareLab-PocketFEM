package com.example.softwarelab.androidbasedfem.Fragments;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.example.softwarelab.androidbasedfem.R;
import com.example.softwarelab.androidbasedfem.Rendering.Renderer;


public class fragment_StartingPage extends android.support.v4.app.Fragment {
    private GLSurfaceView glSurfaceView;
    private Renderer renderer;
    private static final String TAG = "MyActivity";



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){

        View fragmentStartingPageView = inflater.inflate(R.layout.layout_startingpage,container,false);

        RelativeLayout frame = fragmentStartingPageView.findViewById(R.id.layout_startingpage);






        return fragmentStartingPageView;
    }


} // end of fragment class
