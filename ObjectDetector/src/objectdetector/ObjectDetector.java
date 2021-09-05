/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package objectdetector;

import classloader.JarClassLoader;
import objectdetector.GUI.Window;

/**
 *
 * @author PrinzKm
 */
public class ObjectDetector {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    //            java.awt.EventQueue.invokeLater(new Runnable() {
      //      public void run() {
              try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }    Window window= new Window();
             window.setVisible(true);
             window.readCam1();     
    }
    
}
