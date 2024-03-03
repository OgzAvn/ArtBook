package com.oguzavanoglu.artbookjava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.oguzavanoglu.artbookjava.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    ArtAdapter artAdapter;
    ArrayList<Art> artArrayList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        artArrayList = new ArrayList<Art>();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        artAdapter = new ArtAdapter(artArrayList);
        binding.recyclerView.setAdapter(artAdapter);

        getData();
    }

    private void getData(){
        try {
            SQLiteDatabase sqLiteDatabase = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM arts",null);
            int nameIx = cursor.getColumnIndex("artname");
            int idIx = cursor.getColumnIndex("id");

            while(cursor.moveToNext()){
                String name = cursor.getString(nameIx);
                int id = cursor.getInt(idIx);
                Art art = new Art(name,id);

                artArrayList.add(art); //Name ve id leri bir list e kaydedelim ki bunu RecyclerView de gösterebilelim.
            }

            artAdapter.notifyDataSetChanged(); //Recyclerview a veri geldi göster dememiz gerekiyor yoksa göstermez.

            cursor.close();

        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }




    //Menüyü burada aktif edebilmek için 2 tane method çağırıp üstüne yazmamız gerekiyor.

    //1.si (oluşturduğumuz menüyü kod a bağlıyoruz.)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Bağalamk için inflate kullanmamız gerekiyor.
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.art_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }



    //2.si menüye tıklayınca ne olacağını söyleyen method
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId()==R.id.add_art)//menu munuz id sini kontrol ediyoruz.
        {
            Intent intent = new Intent(MainActivity.this,ArtActivity.class);
            intent.putExtra("info","new");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}