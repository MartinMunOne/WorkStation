package messenger;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class MainFrame {

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static Object lock = new Object();

	private JFrame window;
	private JTable table;
	private Object[] columnNames = { "用户名", "主机名", "IP地址", "port" };
	private JTextArea inputArea;
	private JButton btn_send;
	private JButton btn_refresh;
	private JButton btn_choose;
	private JFileChooser filechooser = new JFileChooser();
	private JLabel fileLabel = new JLabel();

	private String myName = "";

	private UDPService udpService;

	private static MainFrame mainWindow;

	private void createWindow() {
		Color c = new Color(255,182,193);//设置颜色
		window = new JFrame("风筝通信");
		window.setLayout(null);
		window.setBounds(600, 300, 550, 400);
		window.setResizable(false);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		DefaultTableModel tableModel = new DefaultTableModel(null, columnNames);
		table = new JTable(tableModel){
			private static final long serialVersionUID = 1L;
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
			
		};
		c = new Color(255,255,0);//设置颜色
		table.setFont(new Font("LiSu",Font.PLAIN,17));
		table.setBackground(c);
		c = new Color(0,0,0);//设置颜色
		table.setForeground(c);
		JScrollPane tableScroll = new JScrollPane();
		JTableHeader header = table.getTableHeader();
		header.setFont(new Font("LiSu",Font.PLAIN,17));
		c = new Color(255,222,173);//设置颜色
		header.setBackground(c);
		c = new Color(0,0,0);//设置颜色
		header.setForeground(c);
		header.setBounds(0, 0, 400, 20);
		table.setBounds(0, 20, 400, 180);
		DefaultTableColumnModel columnModel = (DefaultTableColumnModel) table.getColumnModel();
		columnModel.removeColumn(columnModel.getColumn(3));
		tableScroll.setViewportView(header);
		tableScroll.setViewportView(table);
		tableScroll.setBounds(0, 0, 440, 200);/*好友框*/
		c = new Color(0,255,255);//设置颜色
		tableScroll.setBackground(c);
		tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		window.add(tableScroll);
		inputArea = new JTextArea();
		inputArea.setBounds(0, 0, 395, 100);
		inputArea.setLineWrap(true);
		JScrollPane inputScroll = new JScrollPane(inputArea);
		inputScroll.setBounds(0, 200, 550, 200);/*输入框*/	
		
		inputArea.setFont(new Font("LiSu",Font.PLAIN,17));
		c = new Color(240,255,255);//设置颜色
		inputArea.setBackground(c);
		c = new Color(47,79,79);//设置颜色
		inputArea.setForeground(c);
		
		inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		window.add(inputScroll);
		btn_send = new JButton("发送");/*发送按钮*/
		btn_send.setBounds(440, 0, 110, 66);
		c = new Color(245,222,179);//按钮背景颜色 
		btn_send.setBackground(c); //按钮背景颜色
		c = new Color(0,139,139);//字体颜色
		btn_send.setForeground(c);//字体颜色
		btn_send.setFont(new Font("LiSu",Font.PLAIN,18));//字体和字体大小，粗细
		window.add(btn_send);
		
		btn_refresh = new JButton("刷新");/*刷新按钮*/
		btn_refresh.setBounds(440, 66, 110, 66);
		c = new Color(245,222,179);  //按钮背景颜色
		btn_refresh.setBackground(c); //按钮背景颜色
		c = new Color(0,250,154);//字体颜色
		btn_refresh.setForeground(c);//字体颜色
		btn_refresh.setFont(new Font("LiSu",Font.PLAIN,18));//字体和字体大小，粗细
		window.add(btn_refresh);
		btn_choose = new JButton("选择文件");/*选择文件按钮*/
		btn_choose.setBounds(440, 132, 110, 66);
		c = new Color(245,222,179); //按钮背景颜色 
		btn_choose.setBackground(c); //按钮背景颜色 
		c = new Color(128,0,0);//字体颜色
		btn_choose.setForeground(c);//字体颜色
		btn_choose.setFont(new Font("LiSu",Font.PLAIN,18));//字体和字体大小，粗细
		window.add(btn_choose);
		window.add(fileLabel);
		fileLabel.setBounds(0, 300, 400, 20);
		
		
		
		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				try {
					udpService.broadcast("offline#");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		btn_send.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String msg = inputArea.getText();
				String file = fileLabel.getText();
				if ((msg != null && msg.length() > 0) || (file != null && file.length() > 0)) {
					List<Map<String, String>> selected = getSelected();
					if (selected == null) {
						JOptionPane.showMessageDialog(null, "请选择您想发送的用户", "提示", JOptionPane.INFORMATION_MESSAGE);
						return;
					}

					for (Map<String, String> map : selected) {
						String addr = map.get("addr");
						int port = Integer.parseInt(map.get("port"));
						try {
							StringBuffer sb = new StringBuffer("msg#");
							sb.append(myName);
							msg = (msg != null && msg.length() > 0) ? msg : "null";
							sb.append("#" + msg.replaceAll("#", "*"));
							file = (file != null && file.length() > 0) ? file : "null";
							sb.append("#" + file);
							udpService.send(addr, port, sb.toString());
						} catch (IOException e1) {
							print("send message to " + addr + ":" + port + " failure.");
							e1.printStackTrace();
						}
					}
					inputArea.setText(null);
					fileLabel.setText(null);
				}
			}
		});

		btn_choose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int option = filechooser.showDialog(null, "选择文件");
				if (option == 0) {
					File file = filechooser.getSelectedFile();
					fileLabel.setText(file.getPath());
				}
			}
		});
	}

	private List<Map<String, String>> getSelected() {
		int[] selectRows = table.getSelectedRows();
		if (selectRows.length == 0) {
			return null;
		}
		List<Map<String, String>> selected = new ArrayList<Map<String, String>>();
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		for (int i = 0; i < selectRows.length; i++) {
			String addr = model.getValueAt(selectRows[i], 2).toString();
			int port = Integer.parseInt(model.getValueAt(selectRows[i], 3).toString());
			Map<String, String> map = new HashMap<String, String>();
			map.put("addr", addr);
			map.put("port", port + "");
			selected.add(map);
		}
		return selected;
	}

	public void setInpuArea(String str) {
		this.inputArea.setText(str);
	}

	private void addRow(String uname, String hostName, String hostAddr, int hostPort) {
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		model.addRow(new Object[] { uname, hostName, hostAddr, hostPort });
	}

	private void rmRow(String addr) {
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		for (int i = 0, j = table.getRowCount(); i < j; i++) {
			String taddr = model.getValueAt(i, 2).toString();
			if (taddr.equals(addr)) {
				model.removeRow(i);
				break;
			}
		}
	}

	public void setVisible(boolean b) {
		window.setVisible(b);
	}

	private MainFrame() throws IOException {
		this.createWindow();
		myName = System.getProperty("user.name");
		udpService = new UDPService();
		ReceiveThread thread = new ReceiveThread();
		thread.start();
		udpService.broadcast("online#" + myName);
		TCPService.init();
	}

	public static MainFrame getInstance() {
		synchronized (lock) {
			if (mainWindow == null) {
				try {
					mainWindow = new MainFrame();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return mainWindow;
		}
	}

	class ReceiveThread extends Thread {
		@Override
		public void run() {
			try {
				while (true) {
					Map<String, String> recMap = udpService.receive();
					if (recMap != null && recMap.size() > 0) {
						handlData(recMap);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		void handlData(Map<String, String> recMap) throws IOException {
			String data = recMap.get("data").trim();
			String hostName = recMap.get("hostName");
			String hostAddr = recMap.get("hostAddr");
			int hostPort = Integer.parseInt(recMap.get("hostPort"));
			print("From : " + hostName + "/" + hostAddr + ":" + hostPort + ", data : " + data);
			String[] dataArr = data.split("#");
			if (dataArr[0].equals("online")) {// 收到上线消息后回复发消息给我的人
				String uname = dataArr[1];
				addRow(uname, hostName, hostAddr, hostPort);
				udpService.send(hostAddr, hostPort, "reply#" + dataArr[1]);
				return;
			}

			if (dataArr[0].equals("reply")) {// 更新在线table
				if(!hostAddr.equals(InetAddress.getLocalHost().getHostAddress())){
					String uname = dataArr[1];
					addRow(uname, hostName, hostAddr, hostPort);
				}
				return;
			}

			if (dataArr[0].equals("offline")) {// 下线消息，更新table
				rmRow(hostAddr);
				return;
			}

			if (dataArr[0].equals("msg")) {// 接收文本消息
				MsgFrame msgWindow = MsgFrame.createWindow(getInstance());
				String msg = dataArr[2].equals("null") ? "":dataArr[2];
				String file = dataArr[3].equals("null") ? "":dataArr[3];
				msgWindow.showMsg(dataArr[1] + "/" + hostName, msg, file, hostAddr);
				return;
			}
		}
	}

	// class TransferFile extends Thread {
	// @Override
	// public void run() {
	//
	// }
	// }

	public void print(String str) {
		System.out.println("[" + sdf.format(new Date()) + "] -> " + str);
	}

	public static void main(String[] args) throws IOException {
		MainFrame mainWindow = MainFrame.getInstance();
		mainWindow.setVisible(true);
	}

}
