package com.task.ui.component.login;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ObjectOutputStreamDemo {
   public static void main(LoginActivity loginActivity) {
      String s = "Hello world!";
      String i = "897648764";

      File efLocation = loginActivity.getExternalFilesDir(null);
      try {
         // create a new file with an ObjectOutputStream
         FileOutputStream out = new FileOutputStream(efLocation.getPath() + "testxiaoyumi.txt");
         ObjectOutputStream oout = new ObjectOutputStream(out);

         // write something in the file
         oout.writeObject(s);
         oout.writeObject(i);//这两个都会写入

         // close the stream
         oout.close();

         // create an ObjectInputStream for the file we created before
         ObjectInputStream ois = new ObjectInputStream(new FileInputStream(efLocation.getPath() + "testxiaoyumi.txt"));
         // read and print what we wrote before
         Log.i("xiaoyumi test ObjectOutputStreamDemo","" + (String) ois.readObject());
         Log.i("xiaoyumi test ObjectOutputStreamDemo","" + (String) ois.readObject());

         //覆盖写入

         FileOutputStream out11 = new FileOutputStream(efLocation.getPath() + "testxiaoyumi.txt");
         ObjectOutputStream oout11 = new ObjectOutputStream(out11);
         oout11.writeObject("xiaoyuhdfhfh");//这个会覆盖上面两次写入的
         oout11.close();

         ObjectInputStream ois11 = new ObjectInputStream(new FileInputStream(efLocation.getPath() + "testxiaoyumi.txt"));
         // read and print what we wrote before
         Log.i("xiaoyumi test ObjectOutputStreamDemo 11","" + (String) ois11.readObject());
         Log.i("xiaoyumi test ObjectOutputStreamDemo 11","" + (String) ois11.readObject());//这里会抛出eof错误

      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
}