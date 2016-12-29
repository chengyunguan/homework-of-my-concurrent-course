package ticketingsystem;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import ticketingsystem.TicketingDS;
import ticketingsystem.TicketingSystem;

public class Test{
	final static int THREAD = 16;
	final static int TEST = 20000;
	final static int ROUTENUM = 5;
	final static int COACHNUM = 8;
	final static int SEATNUM = 100;
	final static int STATIONNUM = 10;
	final static boolean FLAG_PRINT = false;//�����Ƿ��ӡ��Ϣ���Ƿ�������ЧƱ
	
	public static void main(String[] args) throws InterruptedException, FileNotFoundException {
		System.out.println("Test ... ");
		if(FLAG_PRINT){//��trace��Ϣ��ӡ���ı��ļ���
			FileOutputStream bos = new FileOutputStream("output.txt");  
			System.setOut(new PrintStream(bos));  
			//System.out.println("output to output.txt");
			System.out.println(ROUTENUM);
			System.out.println(COACHNUM);
			System.out.println(SEATNUM);
			System.out.println(STATIONNUM);
		}
		//
		TicketingDS tds = new TicketingDS(ROUTENUM,COACHNUM,SEATNUM,STATIONNUM);
		MyThread[] myThreads = new MyThread[THREAD];
		for(int i = 0;i<myThreads.length;i++)
			myThreads[i] = new MyThread(THREAD,TEST,ROUTENUM,STATIONNUM,COACHNUM,SEATNUM,tds,FLAG_PRINT);
		for(int i = 0;i<myThreads.length;i++){
			myThreads[i].start();
			//myThreads[i].join();
		}
		Thread.sleep(3000);//�ȴ��������н���
		//���������
		long min_start=myThreads[0].startTime;
		for(int i= 1;i<myThreads.length;i++){
			if(myThreads[i].startTime<min_start)
				min_start = myThreads[i].startTime;
		}
		long max_end=myThreads[0].endTime;
		for(int i= 1;i<myThreads.length;i++){
			if(myThreads[i].endTime>max_end)
				max_end = myThreads[i].endTime;
		}
		long average_buy_time=0;
		long average_inquiry_time=0;
		long average_refund_time=0;
		for(int i= 1;i<myThreads.length;i++){
			average_inquiry_time += myThreads[i].inquiry_time;
			average_buy_time += myThreads[i].buy_time;
			average_refund_time += myThreads[i].refund_time;
		}
		System.out.println("buy_function:"+average_buy_time/THREAD+" ns "+"refund_function:"+average_refund_time/THREAD+" ns "+"inquiry_function:"+average_inquiry_time/THREAD+" ns");
		System.out.println("Throughput=" + TEST/((double)(max_end-min_start)/1000));
		if(FLAG_PRINT){	
			for(int k = 1;k<=ROUTENUM;k++){
				for(int i = 1;i<=STATIONNUM;i++){
					for(int j = i+1;j<=STATIONNUM;j++){
						System.out.println("remainTicketFinal "+tds.inquiry(k, i, j)+" "+k+" "+i+" "+j+" ");
					}
				}
			}
		}
	}

}

class MyThread extends Thread{
	//�̡߳�ִ�д����ȼ�¼
	int THREAD;
	int TEST;
	int STATIONNUM;
	int ROUTENUM;
	int THREADNUM;
	int COACHNUM;
	int SEATNUM;
	//ʱ���¼����
	long startTime;
	long endTime;
	long buy_time=0;
	long refund_time=0;
	long inquiry_time=0;
	//������Ϣ
	String passenger;
	int route;
	int departure;
	int arrival;
	int flag;
	boolean flag_print;
	//�������ݽṹ��Ϣ
	TicketingDS tds;
	Random random;
	int exc[]; //���ڿ����߳�ִ�еķ���
	ArrayList<Ticket> tickets;//����ѹ����Ʊ
	/********************
	 * ���캯��
	 * @param THREAD
	 * @param TEST
	 * @param ROUTENUM
	 * @param STATIONNUM
	 * @param tds
	 * @param flag_print
	 */
	public MyThread(int THREAD,int TEST,int ROUTENUM,int STATIONNUM, int COACHNUM, int SEATNUM, TicketingDS tds,boolean flag_print) {
		// TODO Auto-generated constructor stub
		//
		this.flag_print = flag_print;
		this.THREAD = THREAD;
		this.STATIONNUM = STATIONNUM;
		this.TEST = TEST;
		this.tds = tds;
		this.ROUTENUM = ROUTENUM;
		this.COACHNUM = COACHNUM;
		this.SEATNUM = SEATNUM;
		//
		random = new Random();

		tickets = new ArrayList<Ticket>();
		exc = new int[TEST/THREAD];
		int temp;
		for(int i = 0;i<TEST/THREAD;i++){//���ϴ�ƣ����ҳ���ִ��3��������˳��
			exc[i] = i;
		}
		for(int i = 0;i<TEST/THREAD;i++){
			temp = exc[i];
			int a = random.nextInt(TEST/THREAD);
			exc[i] = exc[a];
			exc[a] = temp;
		}
	}
	
	//�������ú���
	private void set(String passenger,int route,int departure,int arrival,int flag){//���ù�Ʊ����Ʊ��Ϣ
		this.flag = flag;
		this.passenger = passenger;
		this.route = route;
		this.arrival = arrival;
		this.departure = departure;
	}
	
	public void run() {
		int n = TEST/THREAD;//ÿ���߳�ִ��3���������ܴ�����ͬ
		int i;
		int record_buy = 0;
		int record_refund = 0;
		int record_inquiry = 0;
		long start;
		
		startTime = System.currentTimeMillis();//�߳̿�ʼִ�в�����ʱ��
		
		for(i=0;i<n;i++){
			//���ó�����Ϣ
			int _depature = random.nextInt(STATIONNUM-1)+1;
			int _arrival = random.nextInt(STATIONNUM-_depature);
			if(_arrival == 0)
				_arrival = _depature+1;
			else
				_arrival = _depature+_arrival;
			set("passenger"+i+"_"+Thread.currentThread().getId(), random.nextInt(ROUTENUM)+1,_depature, _arrival,exc[i]%10);
			//��ʼִ�з���
			if(flag <= 2){//��Ʊ
				record_buy++;
				start = System.nanoTime();
				Ticket ticket =  tds.buyTicket(passenger, route, departure, arrival);
				if(ticket!=null){
					tickets.add(ticket);	
					if(flag_print)
//						System.out.println("TicketBought "+ticket.tid+" "+ticket.passenger+" "+ticket.route+" "+ticket.departure+" "+ticket.arrival+" "+ticket.coach+" "+ticket.seat+" "+start);
						System.out.println("TicketBought "+ticket.tid+" "+ticket.passenger+" "+ticket.route+" "+ticket.departure+" "+ticket.arrival+" "+ticket.coach+" "+ticket.seat+" ");
				}
				else
					System.out.println("BuyTicket Failed,no more tickets");
				buy_time += System.nanoTime()-start;
			}
			
			else if(flag == 3){//��Ʊ
				record_refund++;
				start = System.nanoTime();
				int flag_refund=10;
				if(flag_print){
					flag_refund = random.nextInt(10);
				}
				if(tickets.size()==0){//���û����Ʊ������һ�ſ�Ʊ
					if(tds.refundTicket(null))
						System.out.println("Invalid TicketRefund succeed,wrong ");
				
				}
				else if(flag_print && flag_refund<=5){//�������ɴ�Ʊ������Ʊ
					Ticket refundTicket = new Ticket();
					refundTicket.route = route;
					refundTicket.passenger = passenger;
					refundTicket.departure = departure;
					refundTicket.arrival = arrival;
					refundTicket.coach = random.nextInt(COACHNUM)+1;
					refundTicket.seat = random.nextInt(SEATNUM)+1;
					refundTicket.tid = random.nextInt(200000)+1;
					if(tds.refundTicket(refundTicket)){
						System.out.println("Invalid TicketRefund succeed,wrong ");
					}
				}
				else{//����Ʊ��������ѡһ��Ʊ������Ʊ
					int choice = random.nextInt(tickets.size());//���ѡ��һ��Ʊ
					Ticket ticket = tickets.get(choice);//ȡ��Ʊ���ж�Ӧ��Ʊ
					if(tds.refundTicket(ticket)){//ִ����Ʊ
						if(flag_print)
							//System.out.println("TicketRefund "+ticket.tid+" "+ticket.passenger+" "+ticket.route+" "+ticket.departure+" "+ticket.arrival+" "+ticket.coach+" "+ticket.seat+" "+start);
							System.out.println("TicketRefund "+ticket.tid+" "+ticket.passenger+" "+ticket.route+" "+ticket.departure+" "+ticket.arrival+" "+ticket.coach+" "+ticket.seat+" ");
						tickets.remove(choice);
					}
					else{
						System.out.println("Valid Ticket "+ticket.tid+" "+ticket.passenger+" "+ticket.route+" "+ticket.departure+" "+ticket.arrival+" "+ticket.coach+" "+ticket.seat+" "+"Refund Failed");
						tickets.remove(choice);
					}
				}
				refund_time += System.nanoTime()-start;
			}
			else{//��Ʊ
				record_inquiry++;
				start = System.nanoTime();
				if(flag_print)
					System.out.println("RemainTicket "+tds.inquiry(route, departure, arrival)+" "+route+" "+departure+" "+arrival+" ");
				inquiry_time+=System.nanoTime()-start;
			}
		}
		endTime = System.currentTimeMillis();//�߳̽���ִ�в�����ʱ��
		inquiry_time=inquiry_time/record_inquiry;//����������ƽ��ִ��ʱ��
		refund_time = refund_time/record_refund;
		buy_time = buy_time/record_buy;
		
	}
}
