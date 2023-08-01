package de.uhingen.kielkopf.andreas.tasmoview.grafik;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import de.uhingen.kielkopf.andreas.beans.minijson.*;
import de.uhingen.kielkopf.andreas.tasmoview.Data;
import de.uhingen.kielkopf.andreas.tasmoview.Tasmota;
import de.uhingen.kielkopf.andreas.tasmoview.tasks.TasmoScanner;

public class JPowerPane extends JPanel implements ActionListener {
   private class SetPower extends SwingWorker<HttpResponse<String>, PowerBox> {
//      static final String    ANTWORT=null;
      private final PowerBox box;
      public SetPower(final PowerBox box1) {
         box=box1;
      }
      @Override
      protected HttpResponse<String> doInBackground() throws Exception {
         try {
            final Tasmota tasm=getPowerJList().getSelectedValue();
            if (tasm == null)
               return null;
            Thread.currentThread().setName(this.getClass().getSimpleName() + " " + tasm.ipPart);
            if (box == null)
               return null;
            try {
               StringBuilder befehl=new StringBuilder("power").append(Integer.toString(box.getNr())); // Abfrage
               if (box.warAktiv)
                  befehl.append(" 2");// statt dessen toggle
               return tasm.requests(new String[] {befehl.toString()}).get(1);
            } catch (final Exception e) {}
         } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         return null;
      }
      @Override
      protected void done() {
         try {
            final JsonObject erg=JsonObject.convertToJson(get().body());
            System.out.print(erg);
            if (erg instanceof JsonList jl)
               if (jl.list.get(0) instanceof JsonString js)
                  box.setStatus(js.value);
         } catch (InterruptedException | ExecutionException e) {}
      }
   }
   private static final long         serialVersionUID=-4892909628585749909L;
   static private final String       FRIENDLY_NAME   ="FriendlyName";
   private JPanel                    panel_1;
   private JLabel                    labelModul;
   private JPanel                    panel_2;
   private JLabel                    labelDeviceName;
   private JPanel                    panel_3;
   private JPanel                    panel_4;
   private JList<Tasmota>            powerJlist;
   private JLabel                    lblNewLabel_1;
   private final ArrayList<PowerBox> boxList         =new ArrayList<>();
   /**
    * Create the panel.
    */
   public JPowerPane() {
      setLayout(new BorderLayout());
      final JPanel panel=new JPanel();
      panel.setBorder(new TitledBorder(null, "Tasmota", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      panel.setName("panel");
      add(panel, BorderLayout.CENTER);
      panel.setLayout(new BorderLayout(10, 10));
      panel.add(getPanel_3(), BorderLayout.WEST);
      panel.add(getPanel_4(), BorderLayout.CENTER);
   }
   @SuppressWarnings("resource")
   @Override
   public void actionPerformed(ActionEvent e) {
      final Tasmota tasmota=getPowerJList().getSelectedValue();
      if (tasmota == null)
         return;
      final Object source=e.getSource();
      for (final PowerBox box:boxList)
         if (box.getButton().equals(source))
            TasmoScanner.getPool().submit(new SetPower(box));// automatic execute in threadpool
   }
   private JLabel getLabelDeviceName() {
      if (labelDeviceName == null) {
         labelDeviceName=new JLabel("PowerBox");
         labelDeviceName.setHorizontalAlignment(SwingConstants.CENTER);
         labelDeviceName.setFont(labelDeviceName.getFont().deriveFont(40f));
         labelDeviceName.setName("labelDeviceName");
      }
      return labelDeviceName;
   }
   private JLabel getLabelModul() {
      if (labelModul == null) {
         labelModul=new JLabel("Sonoff 4CH Modul");
         labelModul.setFont(labelModul.getFont().deriveFont(35f));
         labelModul.setHorizontalAlignment(SwingConstants.CENTER);
         labelModul.setName("labelModul");
      }
      return labelModul;
   }
   private JLabel getLblNewLabel_1() {
      if (lblNewLabel_1 == null) {
         lblNewLabel_1=new JLabel("DeviceName");
         lblNewLabel_1.setBorder(new EmptyBorder(10, 10, 0, 10));
         lblNewLabel_1.setFont(lblNewLabel_1.getFont().deriveFont(20f));
         lblNewLabel_1.setName("lblNewLabel_1");
      }
      return lblNewLabel_1;
   }
   private JPanel getPanel_1() {
      if (panel_1 == null) {
         panel_1=new JPanel();
         panel_1.setName("panel_1");
         panel_1.setLayout(new BorderLayout(10, 10));
         panel_1.add(getLabelModul(), BorderLayout.NORTH);
         panel_1.add(getLabelDeviceName(), BorderLayout.SOUTH);
      }
      return panel_1;
   }
   private JPanel getPanel_2() {
      if (panel_2 == null) {
         panel_2=new JPanel();
         panel_2.setBorder(new TitledBorder(null, "Switch", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_2.setName("panel_2");
         panel_2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
      }
      return panel_2;
   }
   private JPanel getPanel_3() {
      if (panel_3 == null) {
         panel_3=new JPanel();
         panel_3.setBorder(new TitledBorder(null, "select Device", TitledBorder.LEADING, TitledBorder.TOP, null, null));
         panel_3.setName("panel_3");
         panel_3.setLayout(new BorderLayout(10, 10));
         panel_3.add(getPowerJList(), BorderLayout.CENTER);
         panel_3.add(getLblNewLabel_1(), BorderLayout.NORTH);
      }
      return panel_3;
   }
   private JPanel getPanel_4() {
      if (panel_4 == null) {
         panel_4=new JPanel();
         panel_4.setName("panel_4");
         panel_4.setLayout(new BorderLayout(10, 10));
         panel_4.add(getPanel_2(), BorderLayout.CENTER);
         panel_4.add(getPanel_1(), BorderLayout.NORTH);
      }
      return panel_4;
   }
   private JList<Tasmota> getPowerJList() {
      if (powerJlist == null) {
         powerJlist=new JList<>(new DefaultListModel<Tasmota>());
         powerJlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         powerJlist.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
               recalculatePowerBox();
         });
         powerJlist.setName("list");
      }
      return powerJlist;
   }
   public void recalculateListe() {
      if (Data.getData().tasmotasD != null) {
         final DefaultListModel<Tasmota> m=(DefaultListModel<Tasmota>) getPowerJList().getModel();
         m.clear();
         for (final Tasmota tasmota:Data.getData().tasmotasD.values())
            m.addElement(tasmota);
      }
   }
   @SuppressWarnings("resource")
   private void recalculatePowerBox() {
      final Tasmota tasm=getPowerJList().getSelectedValue();
      if (tasm == null)
         return;
      getLabelDeviceName().setText(tasm.deviceName);
      if (tasm.moduleTyp == null) {
         if (tasm.jsontree.get("module") instanceof JsonList j2)
            if (j2.list.get(0) instanceof JsonList j3)
               if (j3.list.get(0) instanceof JsonString j4)
                  tasm.moduleTyp=j4.value;
         System.out.println(tasm.moduleTyp);
      }
      getLabelModul().setText(tasm.moduleTyp);
      final LinkedHashSet<String> boxNameList=new LinkedHashSet<>();
      for (final JsonObject j0:tasm.getAll(FRIENDLY_NAME))
         if (j0 instanceof JsonArray j7)
            for (final JsonObject j1:j7.list)
               if (j1 instanceof JsonString jv)
                  boxNameList.add(jv.value);
      final JPanel p=getPanel_2();
      p.removeAll();
      boxList.clear();
      if (boxNameList.isEmpty())
         return;
      if ((boxNameList.size() > 1) || (!tasm.getAll("POWER").isEmpty()) || (!tasm.getAll("POWER1").isEmpty())) {
         int boxNr=1;
         for (final String boxName:boxNameList) {
            final PowerBox box=new PowerBox();
            boxList.add(box);
            box.setName(boxName);
            box.setNr(boxNr);
            box.setStatus(" ? ");
            box.getButton().addActionListener(this);
            TasmoScanner.getPool().submit(new SetPower(box));// automatic execute in threadpool
            p.add(box);
            boxNr++;
         }
      }
      revalidate();
      repaint(100);// Damit die Boxen auch erscheinen
   }
}
