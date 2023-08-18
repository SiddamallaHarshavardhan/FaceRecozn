package com.harsha.facerecozn;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.harsha.facerecozn.face_recognition.FaceClassifier;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

public class Register extends AppCompatActivity {

//    launcher for the gallery starter activity
    ActivityResultLauncher<Intent> toGalleryActivityLauncher=registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode()==RESULT_OK){
                    image_uri = result.getData().getData();
                    Bitmap inputImage = uriToBitmap(image_uri);
                    Bitmap rotated = rotateBitmap(inputImage);
                    image.setImageBitmap(rotated);
                    performDetection(rotated);
                }
            }
        }
        );

    //camera activity launcher
    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getResultCode()==RESULT_OK){
                        Bitmap inputImage = uriToBitmap(image_uri);
                        Bitmap rotated = rotateBitmap(inputImage);
                        image.setImageBitmap(rotated);
                        performDetection(rotated);
                    }
                }
            });



//    face detector
// High-accuracy landmark detection and face classification
FaceDetectorOptions highAccuracyOpts =
        new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
    FaceDetector detector;

    //recognition
    FaceClassifier faceClassifier;


    Uri image_uri;
    Button camera,gallery;
    ImageView image;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FindViewId();
        setOnClickListeners();
        permission();

//        face detector intialization
        detector =FaceDetection.getClient(highAccuracyOpts);
//        recognition


    }




    private void performDetection(Bitmap input){

       Bitmap mutableBITMAP= input.copy(Bitmap.Config.RGB_565,true);
        Canvas canvas=new Canvas(mutableBITMAP);
        InputImage imageInput = InputImage.fromBitmap(input, 0);
        Task<List<Face>> result =
                detector.process(imageInput)
                        .addOnSuccessListener(
                                new OnSuccessListener<List<Face>>() {
                                    @Override
                                    public void onSuccess(List<Face> faces) {
                                        // Task completed successfully


                                        for (Face face : faces) {
                                            Rect bounds = face.getBoundingBox();

                                            Paint p1=new Paint();
                                                p1.setColor(Color.RED);
                                                p1.setStyle(Paint.Style.STROKE);
                                                p1.setStrokeWidth(5);
                                                performRecognition(bounds,input);
                                            canvas.drawRect(bounds,p1);

                                        }
                                        image.setImageBitmap(mutableBITMAP);

                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }



//    method for the face recognition
    public void performRecognition(Rect bound,Bitmap input){
        if(bound.top<0){
            bound.top=0;
        }
        if(bound.left<0){
            bound.left=0;
        }
        if(bound.right>input.getWidth()){
            bound.right=input.getWidth()-1;
        }
        if(bound.bottom>input.getHeight()){
            bound.bottom=input.getHeight()-1;
        }
        Bitmap croppedFace=Bitmap.createBitmap(input,bound.left,bound.top,bound.width(),bound.height());
//        image.setImageBitmap(croppedFace);
        croppedFace=Bitmap.createScaledBitmap(croppedFace,160,160,false);
        FaceClassifier.Recognition recognition=faceClassifier.recognizeImage(croppedFace,true);
        showRegistrationDialog(croppedFace,recognition);
    }


//method to show the dialog of registration
    public void showRegistrationDialog(Bitmap face,FaceClassifier.Recognition recognition){
        Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.activity_registerdialog);
        ImageView imageView1=dialog.findViewById(R.id.Imaggeview_registerdialog);
        EditText editText=dialog.findViewById(R.id.edittext_registerationname);
        Button register=dialog.findViewById(R.id.button_registerDialog);
        imageView1.setImageBitmap(face);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editText.getText().toString().equals("")){
                    editText.setError("Enter Name");
                }else{
                    faceClassifier.register(editText.getText().toString(),recognition);
                    Toast.makeText(Register.this,"Face is registered succesfully",Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }



    private void permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED){
                String[] permission = {android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
                requestPermissions(permission, 112);
            }
        }

    }


    //    method for on click listeners
    private void setOnClickListeners() {
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                toGalleryActivityLauncher.launch(toGallery);
            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
                    String[] permission = {android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    requestPermissions(permission, 112);
                }
                else {
                    openCamera();
                }
            }
        });

    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        cameraActivityResultLauncher.launch(cameraIntent);
    }



//method for open camera

    //    method for finding the id's
    private void FindViewId() {
        camera=findViewById(R.id.button_camera);
        gallery=findViewById(R.id.button_gallery);
        image=findViewById(R.id.imageview_registericon);
    }



//    method for changing uri to bitmap
private Bitmap uriToBitmap(Uri selectedFileUri) {
    try {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(selectedFileUri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    } catch (IOException e) {
        e.printStackTrace();
    }
    return  null;
}

//rotate method for rotating the bitmap to the portrait method even if we have image in landscape
@SuppressLint("Range")
public Bitmap rotateBitmap(Bitmap input){
    String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
    Cursor cur = getContentResolver().query(image_uri, orientationColumn, null, null, null);
    int orientation = -1;
    if (cur != null && cur.moveToFirst()) {
        orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
    }
    Log.d("tryOrientation",orientation+"");
    Matrix rotationMatrix = new Matrix();
    rotationMatrix.setRotate(orientation);
    Bitmap cropped = Bitmap.createBitmap(input,0,0, input.getWidth(), input.getHeight(), rotationMatrix, true);
    return cropped;

}


}
