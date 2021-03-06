/*
 * Copyright (C) 2018 Philip M. Trenwith
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package LuceneIndexer.ui.fx;

import LuceneIndexer.cConfig;
import LuceneIndexer.dialogs.cConfirmDialog;
import LuceneIndexer.injection.cInjector;
import LuceneIndexer.linux.cLinux;
import LuceneIndexer.persistance.cSerializationFactory;
import LuceneIndexer.persistance.cWindowBounds;
import LuceneIndexer.drives.cDriveMediator;
import LuceneIndexer.lucene.cIndex;
import LuceneIndexer.scheduling.cSchedular;
import java.io.File;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Philip Trenwith
 */
public class LuceneIndexerFX extends Application
{
  private cSerializationFactory m_oSerializationFactory = new cSerializationFactory();
  private cMainLayoutController m_oMainLayoutController;
  private cWindowBounds m_oWindowBounds;
  private File m_fBounds;
  public static Stage m_oStage;
  private cSchedular m_oSchedular;

  @Override
  public void start(Stage oStage) throws Exception
  {
    FXMLLoader oLoader = new FXMLLoader(getClass().getResource("/fxml/cMainLayout.fxml"));
    Parent oRoot = oLoader.load();
    Scene oScene = new Scene(oRoot);
    oScene.getStylesheets().add("/styles/Styles.css");
    m_oStage = oStage;
    oStage.setTitle("Lucene Indexer");
    
    m_oWindowBounds = new cWindowBounds();
    m_oWindowBounds.setX(50);
    m_oWindowBounds.setY(50);
    m_oWindowBounds.setW(1200);
    m_oWindowBounds.setH(800);
    m_fBounds = new File("bounds.ser");
    if (m_fBounds.exists())
    {
      m_oWindowBounds = (cWindowBounds) m_oSerializationFactory.deserialize(m_fBounds, false);
    }
    
    oStage.setX(m_oWindowBounds.getX());
    oStage.setY(m_oWindowBounds.getY());
    oStage.setWidth(m_oWindowBounds.getW());
    oStage.setHeight(m_oWindowBounds.getH());
    
    oStage.setScene(oScene);
    oStage.show();
    
    oStage.setOnCloseRequest(new EventHandler<WindowEvent>()
    {
      @Override
      public void handle(WindowEvent t)
      {
        cConfirmDialog oDialog = new cConfirmDialog(m_oStage, "Are you sure you want to exit?");
        oDialog.showAndWait();
        int result = oDialog.getResult();
        if (result == cConfirmDialog.YES)
        {
          terminate();
        }
        else
        {
          t.consume();
        }
      }
    });

    m_oMainLayoutController = oLoader.<cMainLayoutController>getController();
    cInjector oInjector = new cInjector(this, m_oMainLayoutController);
    
    cDriveMediator.instance().loadDrives();
    
    if (cConfig.instance().getEnableScheduler())
    {
      m_oSchedular = new cSchedular(1);
      int iHour = cConfig.instance().getHourOfDay();
      m_oSchedular.runAt(iHour);
    }
  }

  private void terminate()
  {
    m_oWindowBounds.setX((int)m_oStage.getX());
    m_oWindowBounds.setY((int)m_oStage.getY());
    m_oWindowBounds.setH((int)m_oStage.getHeight());
    m_oWindowBounds.setW((int)m_oStage.getWidth());
    m_oSerializationFactory.serialize(m_oWindowBounds, m_fBounds, false);
    
    if (m_oSchedular != null)
    {
      m_oSchedular.terminate();
    }
    
    cDriveMediator.instance().stopScan();
    
    String sOperatingSystem = System.getProperty("os.name");
    if (sOperatingSystem.equalsIgnoreCase("Linux"))
    {
      cLinux.unmountMountedDrives();
    }
    cIndex.closeIndexs();
    System.exit(0);
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {    
    launch(args);
  }
}
