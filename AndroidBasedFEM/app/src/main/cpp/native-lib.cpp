#include <jni.h>
#include <string>

#include <iostream>
#include <fstream>
#include <ctime>
#include "FEMPackage/Geokernel/ConvexHull.h"
#include "FEMPackage/Utilities/Bithacks.h"
#include "FEMPackage/Geokernel/GeometryTypes.h"
#include "FEMPackage/Femkernel/Assembler.h"
#include "FEMPackage/Geokernel/Rectangle.h"
#include "FEMPackage/Geokernel/Vertex.h"
#include "FEMPackage/Femkernel/Mesher2D.h"
#include "FEMPackage/Femkernel/MeshFactory2D.h"
#include "FEMPackage/Femkernel/TriangularEFT.h"
#include "FEMPackage/Mathkernel/modalAnalysis.h"
#include "FEMPackage/Mathkernel/harmonicSolver.h"
#include "FEMPackage/Utilities/Json.hpp"
#include "FEMPackage/External/Eigen/Eigen/Sparse"
#include "FEMPackage/External/Eigen/Eigen/SparseCholesky"
#include "FEMPackage/Postprocessing/PostProcess.h"

using json = nlohmann::json;
using namespace Eigen;

// Dummy Method
extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_softwarelab_androidbasedfem_MainActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


// generateMesh()
// Takes rawGeo, generates mesh, returns meshedGeo
extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_softwarelab_androidbasedfem_MainActivity_generateMesh(JNIEnv *env, jobject obj, jstring str, jint _numX, jint  _numY)
{
    int numX = (int) _numX;
    int numY = (int) _numY;

    // Passed str is the entire string written in json file
    const jsize len = env->GetStringUTFLength(str);
    const char* strChars = env->GetStringUTFChars(str, (jboolean *)0);
    std::string JSONSTR(strChars, len);

    // Start parsing json string
    json JSONOBJ = json::parse(JSONSTR);

    json rawGeoArray = json::array();
    rawGeoArray = JSONOBJ.at("rawGeometries");

    std::string returnString = "";
    Geokernel::Types::GeometryContainer geometry;
    int i=0;
    for (auto& it : rawGeoArray.items())
    {
        i++;
        json element = it.value();
        std::string type = element.at("type");

        if(type.compare("rectangle") == 0)
        {
            json vertexDataX_array = element.at("vertexDataX");
            json vertexDataY_array = element.at("vertexDataY");
            float x1 = vertexDataX_array.at(0);
            float x2 = vertexDataX_array.at(1);

            float y1 = vertexDataY_array.at(0);
            float y2 = vertexDataY_array.at(1);

            Geokernel::Vertex *lowleft = new Geokernel::Vertex(x1, y1);
            Geokernel::Vertex *upright = new Geokernel::Vertex(x2, y2);

            Geokernel::Rectangle *rectangle = new Geokernel::Rectangle(*lowleft, *upright);
            geometry.push_back(std::shared_ptr<Geokernel::AbsGeometry>(rectangle));
        }

        if(type.compare("triangle") == 0)
        {
            json vertexDataX_array = element.at("vertexDataX");
            json vertexDataY_array = element.at("vertexDataY");
            float x1 = vertexDataX_array.at(0);
            float x2 = vertexDataX_array.at(1);
            float x3 = vertexDataX_array.at(2);

            float y1 = vertexDataY_array.at(0);
            float y2 = vertexDataY_array.at(1);
            float y3 = vertexDataY_array.at(2);

            //construct triangle and push back to geometry
            Geokernel::Vertex *v1 = new Geokernel::Vertex(x1, y1);
            Geokernel::Vertex *v2 = new Geokernel::Vertex(x2, y2);
            Geokernel::Vertex *v3 = new Geokernel::Vertex(x3, y3);

            Geokernel::Triangle *triangle = new Geokernel::Triangle(*v1, *v2, *v3);
            geometry.push_back(std::shared_ptr<Geokernel::AbsGeometry>(triangle));
        }

    } // end of iteration over elements

    Geokernel::Types::GeometryContainer meshedGeo;
    Femkernel::TriangularEFT EFT;

    Femkernel::MeshFactory2D* meshFactory2D = new Femkernel::MeshFactory2D(geometry, meshedGeo, EFT, numX, numY);
    Femkernel::Mesher2D* mesher2D = new Femkernel::Mesher2D();
    size_t numOfDoF = 0;
    json meshingStatus;

    meshFactory2D->register_mesher(mesher2D);
    try
    {
        numOfDoF = 2* meshFactory2D->generate_mesh();
    }
    catch (std::string message)
    {
        std::string status =  "Failure! Meshing Error: " + message;

        meshingStatus["status"] = status;
        returnString = meshingStatus.dump();
        return env->NewStringUTF(returnString.c_str());
    }

    // Start building json file for meshed geometries
    json finalMeshedGeoJsonObj;
    json meshedGeoJsonObj;
    json meshedGeoElementsObj;

    meshedGeoJsonObj["numOfDof"] = numOfDoF;
    meshedGeoElementsObj = json::array();

    json tempElementObj;
    json tempVertexDataXJsonArray;
    json tempVertexDataYJsonArray;

    int elementIndex = 0;
    Femkernel::TriangularEFT triangularEFT = mesher2D->getTriangularEFT();
    std::array<size_t, 6> tempEFTtableArray;
    json tempEFTarrayObj = json::array();

    for(const auto &it : meshedGeo)
    {
        Geokernel::Types::VertexContainer tempVertexContainer;
        it.get()->get_vector_vertex(tempVertexContainer);

        for(const auto &itt : tempVertexContainer)
        {
            tempVertexDataXJsonArray.push_back(itt.get_coordinates()._x);
            tempVertexDataYJsonArray.push_back(itt.get_coordinates()._y);
        }

        tempEFTtableArray = triangularEFT.get_element(it);
        for(const auto &ittt: tempEFTtableArray)
        {
            tempEFTarrayObj.push_back(ittt);
        }

        tempElementObj["type"] = "triangle";
        tempElementObj["vertexDataX"] = tempVertexDataXJsonArray;
        tempElementObj["vertexDataY"] = tempVertexDataYJsonArray;
        tempElementObj["EFT"] = tempEFTarrayObj;

        meshedGeoElementsObj.push_back(tempElementObj);
        tempVertexDataXJsonArray.clear();
        tempVertexDataYJsonArray.clear();
        tempElementObj.clear();
        tempEFTarrayObj.clear();
        elementIndex++;
    }

    meshedGeoJsonObj["elements"] = meshedGeoElementsObj;
    finalMeshedGeoJsonObj["status"] = "Success!";
    finalMeshedGeoJsonObj["meshedGeometry"] = meshedGeoJsonObj;
    returnString += finalMeshedGeoJsonObj.dump();

    // Return the json of meshed geometries as a string
    return env->NewStringUTF(returnString.c_str());
}

// solveStaticSystem()
// Takes meshedGeo, material, returns solution to be used in post processing
extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_softwarelab_androidbasedfem_MainActivity_solveStaticSystem(JNIEnv *env, jobject obj, jstring strMeshedGeo, jstring strMaterial, jstring bc)
{

    // Passed str is the entire string written in json file
    const jsize len_material = env->GetStringUTFLength(strMaterial);
    const char* strMaterialChars = env->GetStringUTFChars(strMaterial, (jboolean *)0);
    std::string JSONSTR_material(strMaterialChars, len_material);

    // Start parsing json string
    json JSONOBJ_material = json::parse(JSONSTR_material);
    json materialObj = JSONOBJ_material.at("material");

    float YoungModulus = materialObj.at("YoungsModulus");
    float PoissonsRatio = materialObj.at("PoissonsRatio");
    float Thickness = materialObj.at("Thickness");
    float Density = materialObj.at("Density");

    // Passed str is the entire string written in json file
    const jsize len = env->GetStringUTFLength(strMeshedGeo);
    const char* strChars = env->GetStringUTFChars(strMeshedGeo, (jboolean *)0);
    std::string JSONSTR_meshedGeo(strChars, len);

    // Start parsing json string
    json JSONOBJ = json::parse(JSONSTR_meshedGeo);

    json meshedGeoObj = JSONOBJ.at("meshedGeometry");
    json meshedGeoArray = json::array();
    meshedGeoArray = meshedGeoObj.at("elements");
    int numOfDoF = meshedGeoObj.at("numOfDof");

    std::string returnString = "";

    Geokernel::Types::GeometryContainer meshedGeometry;
    Femkernel::TriangularEFT EFT;

    int i=0;
    for (auto& it : meshedGeoArray.items())
    {
        i++;
        json element = it.value();
        std::string type = element.at("type");

        if(type.compare("triangle") == 0)
        {
            json vertexDataX_array = element.at("vertexDataX");
            json vertexDataY_array = element.at("vertexDataY");
            float x1 = vertexDataX_array.at(0);
            float x2 = vertexDataX_array.at(1);
            float x3 = vertexDataX_array.at(2);

            float y1 = vertexDataY_array.at(0);
            float y2 = vertexDataY_array.at(1);
            float y3 = vertexDataY_array.at(2);

            //construct triangle and push back to geometry
            Geokernel::Vertex *v1 = new Geokernel::Vertex(x1, y1);
            Geokernel::Vertex *v2 = new Geokernel::Vertex(x2, y2);
            Geokernel::Vertex *v3 = new Geokernel::Vertex(x3, y3);

            Geokernel::Triangle *tempTriangle = new Geokernel::Triangle(*v1, *v2, *v3);
            meshedGeometry.push_back(std::shared_ptr<Geokernel::AbsGeometry>(tempTriangle));

            json EFT_arrayObj = element.at("EFT");
            Femkernel::Types::ElementFreedoms *elementFreedoms = new Femkernel::Types::ElementFreedoms();
            for(int idx=0; idx<EFT_arrayObj.size(); idx++)
            {
                (*elementFreedoms)[idx] = EFT_arrayObj.at(idx);
            }

            EFT.register_element(std::shared_ptr<Geokernel::AbsGeometry>(tempTriangle), elementFreedoms);

        }

    } // end of iteration over elements
    // End of parsing EFT and meshedGeo

    // Start assembly
    SparseMatrix<float> K(numOfDoF,numOfDoF);
    SparseMatrix<float> M(numOfDoF,numOfDoF);

    SparseVector<float> F(numOfDoF);
    std::vector<int> pdof;

    Femkernel::Assembler* assembler = new Femkernel::Assembler(YoungModulus, PoissonsRatio, Density, Thickness);
    assembler->start_assembly(meshedGeometry, EFT, K, M);

    // Passed str is the entire string written in json file
    const jsize len_bc = env->GetStringUTFLength(bc);
    const char* strBCChars = env->GetStringUTFChars(bc, (jboolean *)0);
    std::string JSONSTR_BC(strBCChars, len_bc);

    // Start parsing json string
    json JSONOBJ_bc = json::parse(JSONSTR_BC);
    json bcObj = JSONOBJ_bc.at("BC");

    json fixedArray = json::array();
    fixedArray = bcObj.at("Fix");

    json forceObj = bcObj.at("Force");
    json forceDofArray = json::array();
    json forceValuesArray = json::array();
    forceDofArray = forceObj.at("DoFs");
    forceValuesArray = forceObj.at("Values");

    // Apply BC's
    for (auto& it : fixedArray.items())
    {
        pdof.push_back(it.value());
    }

    for(int ii=0; ii < forceDofArray.size(); ii++)
    {
        int tempInt = forceDofArray.at(ii);
        F.coeffRef(tempInt-1) = forceValuesArray.at(ii);
    }

    assembler->apply_boundary_conditions(pdof, K, M, F);

    SimplicialLLT<SparseMatrix<float>> solver;
    SparseMatrix<float> I(numOfDoF,numOfDoF);
    I.setIdentity();
    solver.compute(K);

    if(solver.info()!=Success)
    {
        json displacementJsonObj;
        displacementJsonObj["Status"] = "Failure! Error: Matrix is not invertible!";
        displacementJsonObj["Displacement"] = "";
        returnString = displacementJsonObj.dump();
        return env->NewStringUTF(returnString.c_str());
    }

    SparseVector<float> disp = solver.solve(I)*F;

    // Start building json file for meshed geometries
    json displacementJsonObj;

    json tmpValuesJsonObj;
    tmpValuesJsonObj = json::array();



    for( int i = 0; i < numOfDoF; i++ )
    {
        tmpValuesJsonObj.push_back( disp.coeff( i ) );
    }

    displacementJsonObj["Displacement"] = tmpValuesJsonObj;
    displacementJsonObj["Status"] = "Success!";

    returnString = displacementJsonObj.dump();

    return env->NewStringUTF(returnString.c_str());
}


// solveDynamicSystem()
// Takes meshedGeo, material, returns solution to be used in post processing
extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_softwarelab_androidbasedfem_MainActivity_solveDynamicSystem(JNIEnv *env, jobject obj, jstring strMeshedGeo, jstring strMaterial, jstring bc, jstring modalsettings)
{
    // Passed str is the entire string written in json file
    const jsize len_modal = env->GetStringUTFLength(modalsettings);
    const char* strModalChars = env->GetStringUTFChars(modalsettings, (jboolean *)0);
    std::string JSONSTR_modal(strModalChars, len_modal);

    // Start parsing json string
    json JSONOBJ_modal = json::parse(JSONSTR_modal);
    json modalObj = JSONOBJ_modal.at("ModalSettings");

    int numOfEigen = modalObj.at("NumOfEigen");
    float freq_min = modalObj.at("Freq_min");
    float freq_max = modalObj.at("Freq_max");
    int freq_no = modalObj.at("Freq_no");
    float D1 = modalObj.at("Damping1");
    float D2 = modalObj.at("Damping2");
    bool withDamping = modalObj.at("withDamping");

    // Passed str is the entire string written in json file
    const jsize len_material = env->GetStringUTFLength(strMaterial);
    const char* strMaterialChars = env->GetStringUTFChars(strMaterial, (jboolean *)0);
    std::string JSONSTR_material(strMaterialChars, len_material);

    // Start parsing json string
    json JSONOBJ_material = json::parse(JSONSTR_material);
    json materialObj = JSONOBJ_material.at("material");

    float YoungModulus = materialObj.at("YoungsModulus");
    float PoissonsRatio = materialObj.at("PoissonsRatio");
    float Thickness = materialObj.at("Thickness");
    float Density = materialObj.at("Density");

    // Passed str is the entire string written in json file
    const jsize len = env->GetStringUTFLength(strMeshedGeo);
    const char* strChars = env->GetStringUTFChars(strMeshedGeo, (jboolean *)0);
    std::string JSONSTR_meshedGeo(strChars, len);

    // Start parsing json string
    json JSONOBJ = json::parse(JSONSTR_meshedGeo);

    json meshedGeoObj = JSONOBJ.at("meshedGeometry");
    json meshedGeoArray = json::array();
    meshedGeoArray = meshedGeoObj.at("elements");
    int numOfDoF = meshedGeoObj.at("numOfDof");

    std::string returnString = "";

    Geokernel::Types::GeometryContainer meshedGeometry;
    Femkernel::TriangularEFT EFT;

    int i=0;
    for (auto& it : meshedGeoArray.items())
    {
        i++;
        json element = it.value();
        std::string type = element.at("type");

        if(type.compare("triangle") == 0)
        {
            json vertexDataX_array = element.at("vertexDataX");
            json vertexDataY_array = element.at("vertexDataY");
            float x1 = vertexDataX_array.at(0);
            float x2 = vertexDataX_array.at(1);
            float x3 = vertexDataX_array.at(2);

            float y1 = vertexDataY_array.at(0);
            float y2 = vertexDataY_array.at(1);
            float y3 = vertexDataY_array.at(2);

            //construct triangle and push back to geometry
            Geokernel::Vertex *v1 = new Geokernel::Vertex(x1, y1);
            Geokernel::Vertex *v2 = new Geokernel::Vertex(x2, y2);
            Geokernel::Vertex *v3 = new Geokernel::Vertex(x3, y3);

            Geokernel::Triangle *tempTriangle = new Geokernel::Triangle(*v1, *v2, *v3);
            meshedGeometry.push_back(std::shared_ptr<Geokernel::AbsGeometry>(tempTriangle));

            json EFT_arrayObj = element.at("EFT");
            Femkernel::Types::ElementFreedoms *elementFreedoms = new Femkernel::Types::ElementFreedoms();
            for(int idx=0; idx<EFT_arrayObj.size(); idx++)
            {
                (*elementFreedoms)[idx] = EFT_arrayObj.at(idx);
            }

            EFT.register_element(std::shared_ptr<Geokernel::AbsGeometry>(tempTriangle), elementFreedoms);

        }

    } // end of iteration over elements
    // End of parsing EFT and meshedGeo

    // Start assembly
    SparseMatrix<float> K(numOfDoF,numOfDoF);
    SparseMatrix<float> M(numOfDoF,numOfDoF);

    SparseVector<float> F(numOfDoF);
    std::vector<int> pdof;

    Femkernel::Assembler* assembler = new Femkernel::Assembler(YoungModulus, PoissonsRatio, Density, Thickness);
    assembler->start_assembly(meshedGeometry, EFT, K, M);

    // Passed str is the entire string written in json file
    const jsize len_bc = env->GetStringUTFLength(bc);
    const char* strBCChars = env->GetStringUTFChars(bc, (jboolean *)0);
    std::string JSONSTR_BC(strBCChars, len_bc);

    // Start parsing json string
    json JSONOBJ_bc = json::parse(JSONSTR_BC);
    json bcObj = JSONOBJ_bc.at("BC");

    json fixedArray = json::array();
    fixedArray = bcObj.at("Fix");

    json forceObj = bcObj.at("Force");
    json forceDofArray = json::array();
    json forceValuesArray = json::array();
    forceDofArray = forceObj.at("DoFs");
    forceValuesArray = forceObj.at("Values");

    // Apply BC's
    for (auto& it : fixedArray.items())
    {
        pdof.push_back(it.value());
    }

    for(int ii=0; ii < forceDofArray.size(); ii++)
    {
        int tempInt = forceDofArray.at(ii);
        F.coeffRef(tempInt-1) = forceValuesArray.at(ii);
    }

    /*numOfDoF -= pdof.size();
    SparseMatrix<float> K_m(numOfDoF,numOfDoF);
    SparseMatrix<float> M_m(numOfDoF,numOfDoF);

    SparseVector<float> F_m(numOfDoF);*/

    assembler->apply_boundary_conditions(pdof, K, M, F );


    // Start Solving
//    // Test
//    Eigen::SparseMatrix<float> K_(3,3);
//    Eigen::SparseMatrix<float> M_(3,3);
//    Eigen::SparseVector<float> F_(3);
//
//    ////M = MatrixXd::Zero(3,3);
//
//
//    M_.insert(0,0) = 1;
//    M_.insert(1,1) = 50.0;
//    M_.insert(2,2) = 10.0;
//
//    K_.insert(0,0) = 10000000000;
//    K_.insert(1,1) = 15.0;
//    K_.insert(2,2) = 5;
//    K_.insert(1,2) = -5;
//    K_.insert(2,1) = -5;
//
//
//    F_.insert(0) = 0.0;
//    F_.insert(2) = 1.0;

//    returnString = "";
//    for(int i=0; i<M.rows(); i++)
//    {
//        for(int j=0; j<M.cols(); j++)
//        {
//            returnString += std::to_string( M.coeff(i,j)/1e6f ) + ", ";
//        }
//        returnString +="     ";
//    }

    // Modal Analysis Starts
    Mathkernel::ModalAnalysis modalAnalysis( numOfDoF );
    modalAnalysis.SolveEigenvalueProblem(K, M, numOfEigen + pdof.size() );
    MatrixXf displacementModal = MatrixXf::Zero(numOfDoF,freq_no);
    modalAnalysis.computeModalMatrices(K,M,F);
    if( withDamping )
    {
        modalAnalysis.computeRayleighCoeffs(D1,D2, (int)pdof.size());
        modalAnalysis.computeDisplacementsWithDamping(freq_min,freq_max,freq_no,displacementModal);

    } else
    {
        modalAnalysis.computeDisplacements(freq_min,freq_max,freq_no,displacementModal);
    }


    for( int i = 0; i < pdof.size(); i++ )
    {
        displacementModal.row( pdof[i] - 1 ).setZero();
    }

    // Start building json file for meshed geometries
    json resultsModalAnalysisJsonObj;

    json tmpDispJsonObj = json::array();
    json tmpArrayJsonObj = json::array();

    json tmpFrequJsonObj = json::array();

    json tmpEigenJsonObj = json::array();

    for( int i = 0; i < freq_no; i++ )
    {
        for( int j = 0; j < numOfDoF; j++)
        {
            float a = displacementModal.coeff( j,i );
            std::cout << a << std::endl;
            tmpDispJsonObj.push_back( displacementModal.coeff( j,i ) );
        }
        tmpArrayJsonObj.push_back( tmpDispJsonObj );
        tmpDispJsonObj.clear();

        tmpFrequJsonObj.push_back( modalAnalysis.getFrequencieAt(i));
    }


    for( int i = 0; i < ( numOfEigen + pdof.size() ); i++)
    {
        tmpEigenJsonObj.push_back( modalAnalysis.getEigenfrequenciesHzAt( i ) );
        double a = modalAnalysis.getEigenfrequenciesHzAt(i);

    }

    if( isnan( modalAnalysis.getEigenfrequenciesHzAt( numOfEigen )) )
    {
        resultsModalAnalysisJsonObj["additionalEigenvalue"] = false;
    } else
    {
        resultsModalAnalysisJsonObj["additionalEigenvalue"] = true;
        tmpEigenJsonObj.push_back( modalAnalysis.getEigenfrequenciesHzAt( numOfEigen ) );
    }

    resultsModalAnalysisJsonObj["Displacement"] = tmpArrayJsonObj;
    tmpArrayJsonObj.clear();
    resultsModalAnalysisJsonObj["Frequencies"] = tmpFrequJsonObj;
    tmpFrequJsonObj.clear();
    resultsModalAnalysisJsonObj["Eigenfrequencies"] = tmpEigenJsonObj;
    tmpEigenJsonObj.clear();
    resultsModalAnalysisJsonObj["numEigenvalues"] = numOfEigen;
    resultsModalAnalysisJsonObj["numFrequencies"] = freq_no;

    resultsModalAnalysisJsonObj["Status"] = "Success!";

    returnString = resultsModalAnalysisJsonObj.dump();


    // Return the json of meshed geometries as a string
    return env->NewStringUTF(returnString.c_str());

}


// postProcess()
// Takes meshedGeo, material, returns solution to be used in post processing
extern "C" JNIEXPORT jstring
JNICALL
Java_com_example_softwarelab_androidbasedfem_MainActivity_postProcess(JNIEnv *env, jobject obj, jstring strMeshedGeo, jstring strMaterial, jstring strDisp)
{

    // Passed str is the entire string written in json file
    const jsize len_material = env->GetStringUTFLength(strMaterial);
    const char* strMaterialChars = env->GetStringUTFChars(strMaterial, (jboolean *)0);
    std::string JSONSTR_material(strMaterialChars, len_material);

    // Start parsing json string
    json JSONOBJ_material = json::parse(JSONSTR_material);
    json materialObj = JSONOBJ_material.at("material");

    float YoungModulus = materialObj.at("YoungsModulus");
    float PoissonsRatio = materialObj.at("PoissonsRatio");
    float Thickness = materialObj.at("Thickness");
    float Density = materialObj.at("Density");

    // Passed str is the entire string written in json file
    const jsize len = env->GetStringUTFLength(strMeshedGeo);
    const char* strChars = env->GetStringUTFChars(strMeshedGeo, (jboolean *)0);
    std::string JSONSTR_meshedGeo(strChars, len);

    // Start parsing json string
    json JSONOBJ = json::parse(JSONSTR_meshedGeo);

    json meshedGeoObj = JSONOBJ.at("meshedGeometry");
    json meshedGeoArray = json::array();
    meshedGeoArray = meshedGeoObj.at("elements");
    int numOfDoF = meshedGeoObj.at("numOfDof");

    Geokernel::Types::GeometryContainer meshedGeometry;
    Femkernel::TriangularEFT EFT;

    int i=0;
    for (auto& it : meshedGeoArray.items())
    {
        i++;
        json element = it.value();
        std::string type = element.at("type");

        if(type.compare("triangle") == 0)
        {
            json vertexDataX_array = element.at("vertexDataX");
            json vertexDataY_array = element.at("vertexDataY");
            float x1 = vertexDataX_array.at(0);
            float x2 = vertexDataX_array.at(1);
            float x3 = vertexDataX_array.at(2);

            float y1 = vertexDataY_array.at(0);
            float y2 = vertexDataY_array.at(1);
            float y3 = vertexDataY_array.at(2);

            //construct triangle and push back to geometry
            Geokernel::Vertex *v1 = new Geokernel::Vertex(x1, y1);
            Geokernel::Vertex *v2 = new Geokernel::Vertex(x2, y2);
            Geokernel::Vertex *v3 = new Geokernel::Vertex(x3, y3);

            Geokernel::Triangle *tempTriangle = new Geokernel::Triangle(*v1, *v2, *v3);
            meshedGeometry.push_back(std::shared_ptr<Geokernel::AbsGeometry>(tempTriangle));

            json EFT_arrayObj = element.at("EFT");
            Femkernel::Types::ElementFreedoms *elementFreedoms = new Femkernel::Types::ElementFreedoms();
            for(int idx=0; idx<EFT_arrayObj.size(); idx++)
            {
                (*elementFreedoms)[idx] = EFT_arrayObj.at(idx);
            }

            EFT.register_element(std::shared_ptr<Geokernel::AbsGeometry>(tempTriangle), elementFreedoms);

        }

    } // end of iteration over elements
    // End of parsing EFT and meshedGeo

    // Passed str is the entire string written in json file
    const jsize len_disp = env->GetStringUTFLength(strDisp);
    const char* strDispChars = env->GetStringUTFChars(strDisp, (jboolean *)0);
    std::string JSONSTR_disp(strDispChars, len_disp);

    // Start parsing json string
    json JSONOBJ_disp = json::parse(JSONSTR_disp);
    json dispArrayObj = json::array();

    dispArrayObj = JSONOBJ_disp.at("Displacement");
    std::string displacementStatus = JSONOBJ_disp.at("Status");

    SparseVector<float> displacement(numOfDoF);
    for(int idx=0; idx<dispArrayObj.size(); idx++)
    {
        float tempDisp = dispArrayObj.at(idx);
        displacement.coeffRef(idx) = tempDisp;
    }
//
//    for(int i=0; i<displacement.rows(); i++)
//    {
//        returnString += std::to_string(displacement.coeff(i)) + "  ";
//    }

    Postprocessing::PostProcessor postProcessor(meshedGeometry, EFT,  displacement, YoungModulus, PoissonsRatio, Density, Thickness);
    postProcessor.post_process();


    json postProcessedDataObj;
    json dataArray = json::array();

    for(const auto &el : meshedGeometry)
    {
        json tempElementData = json::array();
        std::array<float ,8> tempData = postProcessor.get_post_processed_result(el);
        std::cout << tempData.at(0);

        for(int idx=0; idx<8; idx++)
        {
            tempElementData.push_back( tempData.at(idx) );
        }

        dataArray.push_back(tempElementData);
    }

    postProcessedDataObj["PostProcessedData"] = dataArray;
    std::string returnString = postProcessedDataObj.dump();

    return env->NewStringUTF(returnString.c_str());
}

