package com.oguzavanoglu.artbookjava;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.TokenWatcher;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.oguzavanoglu.artbookjava.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    Bitmap selectedImage;

    SQLiteDatabase database;

    ActivityResultLauncher<Intent> activityResultLauncher; //Bununla galeri ye gideceğiz.Activite de galeri ye gidip geri geleceğiz.
    ActivityResultLauncher<String> permissionLauncher; //Bununla izin isteyeceğiz. İzinlerde string ile uğraşıyoruz.


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if(info.equals("new")){
            //new art
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.select);

        }else{
            //old art
            int artId = intent.getIntExtra("artId", 0);//defaultValue : EĞer integer bulamazsa yani artId diye bişey yollanmamışsa hangi değeri kullanayım diyor @null gelmemesi için.
            binding.button.setVisibility(View.INVISIBLE);

            //Bana bir id yollandı bu id ye göre veriyi çekecepim ve kullanıcıya göstereceğim.

            try{

                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[]{String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("name");
                int painterNameIx = cursor.getColumnIndex("artistName");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while(cursor.moveToNext()){

                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));


                    //Resmi bize byte olarak gönderiyor. ona göre almamız gerekiyor.
                    byte[] bytes = cursor.getBlob(imageIx);

                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);

                    binding.imageView.setImageBitmap(bitmap);

                }
                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    public void save(View view){
        //SQL ile kaydedeceğiz. Image kaydetmeyi ilk defa burada göreceğiz. Görselin boyutu çok önemli.
        //Verileri String e çevirip aldık
        String name = binding.nameText.getText().toString();
        String artistName = binding.artistText.getText().toString();
        String year = binding.yearText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);
        //Image i veriye çevirmemiz lazım SQL içerisene koymak için 1 lere 0 lara çevireceğiz.

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray(); //Byte[] dizisine byteArray olarak kaydettik.

        try {

            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,name VARCHAR,artistName VARCHAR, year VARCHAR, image BLOB)");
            String sqlString = "INSERT INTO arts(name,artistName,year,image) VALUES(?,?,?,?)";
             //SQLiteStatement bizim verit abanınımızda sql çalıştırmaya çalışırken sonradan bağlama binding işlemlerimi kolay yapmaya yarayan bir yapı
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);//sqlString i alıp database içinde çalıştırmak istedik ve artık farklı değerileri aşağıdaki gibi bağlıyabiliyoruz.
            sqLiteStatement.bindString(1, name);
            sqLiteStatement.bindString(2, artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();


        }catch (Exception e){
            e.printStackTrace();
        }

        //Kayıt olduktan sonra mainactivity e geri dönceğğiz.
        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//Bundan önceki aktiviteleri kapat sadece gideceğim yeri çalıştır.
        startActivity(intent);

    }

    public Bitmap makeSmallerImage(Bitmap image,int maximumSize ){

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / height;

        if(bitmapRatio > 1) {
            //Landscape image Yatay görsel
            width = maximumSize;
            height = (int)(width / bitmapRatio);

        }else{
            //Portrait Dikey bir görsel
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return image.createScaledBitmap(image,width,height,true); //Scale edilmiş ayarlanmış daha büyük veya daha küçük bitmap döndür demek.
    }
    public void selectImage(View view) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //Android 33 -> READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {//1.Daha önce izin alınmışmı bakıyoruz.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {//Kullanıcıya bir izin menüsü gösterilim mi?İzin vermezse bir açıklama yapalım mı?
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //İzin verilmemiş mi eğer verilmemişse izin isteyeceğiz //Request Permission
                            //İzin istemem için permissionLauncher ı kullanmam gerek.
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    }).show();
                } else {
                    //İzin verilmemiş mi eğer verilmemişse izin isteyeceğiz //Request Permission
                    //İzin istemem için permissionLauncher ı kullanmam gerek.
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }


            } else {//Else giriyorsa izin verilmiş
                //Gallery e gidecek direk.
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//Görseli alıp geri geleceğim.
                //Galeriye gitmek için activityResultLauncher ı kullanmam gerek.
                activityResultLauncher.launch(intentToGallery);
            }
        }
        else{//Android 32 <- READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {//1.Daha önce izin alınmışmı bakıyoruz.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {//Kullanıcıya bir izin menüsü gösterilim mi?İzin vermezse bir açıklama yapalım mı?
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //İzin verilmemiş mi eğer verilmemişse izin isteyeceğiz //Request Permission
                            //İzin istemem için permissionLauncher ı kullanmam gerek.
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }).show();
                } else {
                    //İzin verilmemiş mi eğer verilmemişse izin isteyeceğiz //Request Permission
                    //İzin istemem için permissionLauncher ı kullanmam gerek.
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }


            } else {//Else giriyorsa izin verilmiş
                //Gallery e gidecek direk.
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//Görseli alıp geri geleceğim.
                //Galeriye gitmek için activityResultLauncher ı kullanmam gerek.
                activityResultLauncher.launch(intentToGallery);
            }

        }
    }


    private void registerLauncher(){//oluşturduğum launch ları kayıt etmem gerek.

        //activityResultLauncer ile galeriye gidip seçip geliyoruz.
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override   //StartActivityForResult --> yeni bir aktivite başlatıyorum ama bir sonuç için başlatıyorum.
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == RESULT_OK){
                    Intent intentFromResult = result.getData();//getData intent i bize veriyor.
                    if(intentFromResult != null){
                        Uri imageData = intentFromResult.getData(); //getData burada bize URI ı veriyor yani seçtiği görselin nerede kayıtlı olduğunu veriyor.
                        //binding.imageView.setImageURI(imageData);

                        //Bitmap e çeviriyoruz çünkü bu resmin verisine ihtiyacım var
                        try{
                            if(Build.VERSION.SDK_INT >= 28) {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            }else{
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        //Sonunda bir cevap alacağımız bir işlem yapacağımızı söylüyoruz burada
        //İzin istemek olabilir veya yeni bir aktiviteye gitmek olabilir. Burada izin istiyoruz.
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) { //result true ise izin verildi demek
                if(result){
                    //Permission granted
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    //Galeriye gitmek için activityResultLauncher ı kullanmam gerek.
                    activityResultLauncher.launch(intentToGallery);

                }else{
                    //Permission denied
                    Toast.makeText(ArtActivity.this, "Permission needed", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}