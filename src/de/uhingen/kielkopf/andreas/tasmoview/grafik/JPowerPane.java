package de.uhingen.kielkopf.andreas.tasmoview.grafik;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonArray;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonList;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonObject;
import de.uhingen.kielkopf.andreas.tasmoview.minijson.JsonString;
import de.uhingen.kielkopf.andreas.tasmoview.tasks.TasmoScanner;

public class JPowerPane extends JPanel implements ActionListener {
   private static final long   serialVersionUID=-4892909628585749909L;
   private JPanel              panel_1;
   private JLabel              labelModul;
   private JPanel              panel_2;
   private JLabel              labelDeviceName;
   private JPanel              panel_3;
   private JPanel              panel_4;
   private JList<Tasmota>      list;
   private JLabel              lblNewLabel_1;
   private ArrayList<PowerBox> boxList         =new ArrayList<>();
   static private final String FRIENDLY_NAME   ="FriendlyName";
   /**
    * Create the panel.
    */
   public JPowerPane() {
      setLayout(new BorderLayout());
      JPanel panel=new JPanel();
      panel.setBorder(new TitledBorder(null, "Tasmota", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      panel.setName("panel");
      add(panel, BorderLayout.CENTER);
      panel.setLayout(new BorderLayout(10, 10));
      panel.add(getPanel_3(), BorderLayout.WEST);
      panel.add(getPanel_4(), BorderLayout.CENTER);
   }
   private JPanel getPanel_1() {
      if (panel_1==null) {
         panel_1=new JPanel();
         panel_1.setName("panel_1");
         panel_1.setLayout(new BorderLayout(10, 10));
         panel_1.add(getLabelModul(), BorderLayout.NORTH);
         panel_1.add(getLabelDeviceName(), BorderLayout.SOUTH);
      }
      return panel_1;
   }
   private JLabel getLabelModul() {
      if (labelModul==null) {
         labelModul=new JLabel("Sonoff 4CH Modul");
         labelModul.setFont(labelModul.getFont().deriveFont(35f));
         labelModul.setHorizontalAlignment(SwingConstants.CENTER);
         labelModul.setName("labelModul");
      }
      return labelModul;
   }
   private JPanel getPanel_2() {
      if (panel_2==null) {
         panel_2=new JPanel();
         panel_2.setBorder(new TitledBorder(null, "Switch", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_2.setName("panel_2");
         panel_2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
      }
      return panel_2;
   }
   private JLabel getLabelDeviceName() {
      if (labelDeviceName==null) {
         labelDeviceName=new JLabel("PowerBox");
         labelDeviceName.setHorizontalAlignment(SwingConstants.CENTER);
         labelDeviceName.setFont(labelDeviceName.getFont().deriveFont(40f));
         labelDeviceName.setName("labelDeviceName");
      }
      return labelDeviceName;
   }
   private JPanel getPanel_3() {
      if (panel_3==null) {
         panel_3=new JPanel();
         panel_3.setBorder(new TitledBorder(null, "select Device", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_3.setName("panel_3");
         panel_3.setLayout(new BorderLayout(10, 10));
         panel_3.add(getList(), BorderLayout.CENTER);
         panel_3.add(getLblNewLabel_1(), BorderLayout.NORTH);
      }
      return panel_3;
   }
   private JPanel getPanel_4() {
      if (panel_4==null) {
         panel_4=new JPanel();
         panel_4.setName("panel_4");
         panel_4.setLayout(new BorderLayout(10, 10));
         panel_4.add(getPanel_2(), BorderLayout.CENTER);
         panel_4.add(getPanel_1(), BorderLayout.NORTH);
      }
      return panel_4;
   }
   private JList<Tasmota> getList() {
      if (list==null) {
         list=new JList<Tasmota>(new DefaultListModel<Tasmota>());
         list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
               if (!e.getValueIsAdjusting()) recalculatePowerBox();
            }
         });
         list.setName("list");
      }
      return list;
   }
   private JLabel getLblNewLabel_1() {
      if (lblNewLabel_1==null) {
         lblNewLabel_1=new JLabel("DeviceName");
         lblNewLabel_1.setBorder(new EmptyBorder(10, 10, 0, 10));
         lblNewLabel_1.setFont(lblNewLabel_1.getFont().deriveFont(20f));
         lblNewLabel_1.setName("lblNewLabel_1");
      }
      return lblNewLabel_1;
   }
   public void recalculateListe() {
      if ((Data.data!=null)&&(Data.data.tasmotas!=null)) {
         DefaultListModel<Tasmota> m=(DefaultListModel<Tasmota>) getList().getModel();
         m.clear();
         for (Tasmota tasmota:Data.data.tasmotas)
            m.addElement(tasmota);
      }
   }
   private void recalculatePowerBox() {
      Tasmota tasm=getList().getSelectedValue();
      if (tasm==null) return;
      getLabelDeviceName().setText(tasm.deviceName);
      if (tasm.moduleTyp==null) {
         JsonObject jm=tasm.jsontree.get("module");
         if (jm instanceof JsonList) {
            JsonObject j2=((JsonList) jm).list.get(0);
            if (j2 instanceof JsonList) {
               JsonObject j4=((JsonList) j2).list.get(0);
               if (j4 instanceof JsonString) tasm.moduleTyp=((JsonString) j4).value;
            }
         }
         System.out.println(tasm.moduleTyp);
      }
      getLabelModul().setText(tasm.moduleTyp);
      LinkedHashSet<String> boxNameList=new LinkedHashSet<>();
      for (JsonObject j0:tasm.getAll(FRIENDLY_NAME))
         if (j0 instanceof JsonArray) for (JsonObject j1:((JsonArray) j0).list)
            if (j1 instanceof JsonString) boxNameList.add(((JsonString) j1).value);
      JPanel p=getPanel_2();
      p.removeAll();
      boxList.clear();
      if (boxNameList.isEmpty()) return;
      if ((boxNameList.size()>1)||(!tasm.getAll("POWER").isEmpty())||(!tasm.getAll("POWER1").isEmpty())) {
         int boxNr=1;
         for (String boxName:boxNameList) {
            PowerBox box=new PowerBox();
            boxList.add(box);
            box.setName(boxName);
            box.setNr(boxNr);
            box.setStatus(" ? ");
            box.getButton().addActionListener(this);
            TasmoScanner.exec.submit(new SetPower(box));// automatic execute in threadpool
            p.add(box);
            boxNr++;
         }
      }
      revalidate();
      repaint(100);// Damit die Boxen auch erscheinen
   }
   @Override
   public void actionPerformed(ActionEvent e) {
      Tasmota tasmota=getList().getSelectedValue();
      if (tasmota==null) return;
      Object source=e.getSource();
      for (PowerBox box:boxList)
         if (box.getButton().equals(source)) TasmoScanner.exec.submit(new SetPower(box));// automatic execute in threadpool
   }
   private class SetPower extends SwingWorker<String, PowerBox> {
      static final String ANTWORT="";
      private PowerBox    box;
      public SetPower(final PowerBox box) {
         this.box=box;
      }
      @Override
      protected String doInBackground() throws Exception {
         Tasmota tasm=getList().getSelectedValue();
         if (tasm==null) return ANTWORT;
         Thread.currentThread().setName(this.getClass().getSimpleName()+" "+tasm.ipPart);
         if (box==null) return ANTWORT;
         try {
            String befehl="power"+Integer.toString(box.getNr()); // Abfrage
            if (box.warAktiv) befehl+=" 2";// statt dessen toggle
            return tasm.requests(new String[] {befehl}).get(1);
         } catch (Exception e) {}
         return ANTWORT;
      }
      @Override
      protected void done() {
         try {
            JsonObject erg=JsonObject.convertToJson(get());
            System.out.print(erg);
            if (erg instanceof JsonList) {
               JsonObject j1=((JsonList) erg).list.get(0);
               if (j1 instanceof JsonString) box.setStatus(((JsonString) j1).value);
            }
         } catch (InterruptedException|ExecutionException e) {}
      }
   }
}
