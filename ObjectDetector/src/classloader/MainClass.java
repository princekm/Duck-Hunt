/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package classloader;

/**
 *
 * @author PrinzKm
 */
public class MainClass {
        public static void main(String[] args) {
        // TODO code application logic here
    //            java.awt.EventQueue.invokeLater(new Runnable() {
      //      public void run() {
        JarClassLoader jcl = new JarClassLoader();
        try {
            jcl.invokeMain("objectdetector.ObjectDetector", args);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        }
}
