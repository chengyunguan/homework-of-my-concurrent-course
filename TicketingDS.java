package ticketingsystem;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.print.attribute.standard.PrinterLocation;

public class TicketingDS implements TicketingSystem {
	int routenum = 5;
	int coachnum = 8;
	int seatnum = 100;
	int stationnum = 10;
	Random random = new Random();
	//
	AtomicLong[] ticket_record;//����tid��
	//
	Train_Info train_Info;
	Seat_Info seat_Info;
	/***********************************************************************
	 * TicketingDS���캯��
	 * @param routenum
	 * @param coachnum
	 * @param seatnum
	 * @param stationnum
	 */
	public TicketingDS(int routenum,int coachnum,int seatnum,int stationnum){
		this.routenum = routenum;
		this.coachnum = coachnum;
		this.seatnum = seatnum;
		this.stationnum = stationnum;
		this.ticket_record = new AtomicLong[routenum+1];//����tid��
		for(int i = 1;i<=routenum;i++){
			ticket_record[i]=new AtomicLong(i*1000000);
		}
		train_Info = new Train_Info(routenum, coachnum, seatnum, stationnum);
		seat_Info = new Seat_Info(routenum,stationnum, coachnum, seatnum);
	}
	/**************************************
	 * �޲������캯��
	 */
	public TicketingDS() {
		this.ticket_record =new AtomicLong[routenum+1];
		for(int i = 1;i<=routenum;i++){
			ticket_record[i]=new AtomicLong(i*1000000);
		}
		train_Info = new Train_Info(routenum,coachnum,seatnum,stationnum);
		seat_Info = new Seat_Info(routenum,stationnum, coachnum, seatnum);
	}
	/***********************************
	��Ʊ����
	***********************************/
	@Override
	public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
		// TODO Auto-generated method stub
		int round_back_i = 0;
		for(int i = departure;i<arrival;i++)
			if(train_Info.train_info[route][i].getAndDecrement()<=0){
				round_back_i = i;
				break;
			}
		if(round_back_i != 0){//��������Ʊ����ʱ���������ʱ�����ڶ�Ӧ������ƱΪ0ʱ��������Ч��ѯ�����
			for(int i = departure;i<=round_back_i;i++)
				train_Info.train_info[route][i].getAndIncrement();
		}
		//******************************************************
		
		boolean flag = false;
		int t = (int) (Thread.currentThread().getId()%coachnum);
		if(t==0) t=random.nextInt(coachnum)+1;
		int z =0;
		for(int i = t;i<coachnum+t;i++){
			if(i>coachnum)
				z = i-coachnum;
			else
				z = i;
			for(int j = 1;j<=seatnum;j++){//����λ,��ʱ�趨Ϊ��ͷ��ʼѰ��	
				try{
				if(seat_Info.Seat_Nodes[route][z][j].Station[departure] || seat_Info.Seat_Nodes[route][z][j].Station[arrival-1]){
					continue;
				}
				}catch (ArrayIndexOutOfBoundsException e) {
					// TODO: handle exception
					System.out.println("hehe");
				}
				try {
					seat_Info.Seat_Nodes[route][z][j].lock.tryLock(2,TimeUnit.SECONDS);//����TryLcok���Ʊ�֤����������2��ȴ�
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for(int k = departure;k<arrival;k++){//����
					if(seat_Info.Seat_Nodes[route][z][j].Station[k]){
						flag = true;//����ʧ��
						break;
					}
				}
				if(flag){
					flag = false;
					seat_Info.Seat_Nodes[route][z][j].lock.unlock();
					continue;
				}
				Ticket newTicket = new Ticket();
				//newTicket.tid = System.nanoTime();//
				newTicket.tid =ticket_record[route].getAndIncrement();//��ȡ��һ��tid��
				newTicket.passenger = passenger;
				newTicket.route = route;
				newTicket.departure = departure;
				newTicket.arrival = arrival;
				newTicket.coach = z;
				newTicket.seat = j;
				for(int k = departure;k<arrival;k++){
					seat_Info.Seat_Nodes[route][z][j].Station[k]=true;
				}
				seat_Info.Seat_Nodes[route][z][j].tid[departure] = newTicket.tid;
				seat_Info.Seat_Nodes[route][z][j].tid[arrival-1] = newTicket.tid;
				seat_Info.Seat_Nodes[route][z][j].passenger[departure] = newTicket.passenger;
				seat_Info.Seat_Nodes[route][z][j].passenger[arrival-1] = newTicket.passenger;
				seat_Info.Seat_Nodes[route][z][j].lock.unlock();
				return newTicket;			
			}	
		}

		return null;		
		
	}
	/*************************************
	 *��Ʊ���� 
	 ************************************/
	@Override
	public int inquiry(int route, int departure, int arrival) {
		// TODO Auto-generated method stub
		//return Math.min(train_Info.train_info[route][departure].get(), train_Info.train_info[route][(route+arrival)/2].get());
		int sum = train_Info.train_info[route][departure].get();
		for(int i = departure+1;i<arrival;i++){
			int a = train_Info.train_info[route][i].get();
			sum = (sum>a?a:sum);
		}
		return sum;
	}
	/***************************************
	 * ��Ʊ����
	 **************************************/
	@Override
	public boolean refundTicket(Ticket ticket) {
		// TODO Auto-generated method stub
		if(ticket == null || ticket.tid != seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].tid[ticket.departure]|| ticket.tid != seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].tid[ticket.arrival-1] 
				|| ticket.passenger != seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].passenger[ticket.departure] || ticket.passenger != seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].passenger[ticket.arrival-1])
			return false;
		seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].lock.lock();
		for(int k = ticket.departure;k<ticket.arrival;k++){
			seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].Station[k]= false;
			train_Info.train_info[ticket.route][k].getAndIncrement();	
		}	
		seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].tid[ticket.departure] = 0;
		seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].tid[ticket.arrival-1] = 0;
		seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].passenger[ticket.departure] = "";
		seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].passenger[ticket.arrival-1] = "";
		seat_Info.Seat_Nodes[ticket.route][ticket.coach][ticket.seat].lock.unlock();
		return true;
	}
}
		
class Train_Info{//�ܱ�,���ڼ�¼�ܵĳ�Ʊ���
	Lock[] train_info_lock;
	AtomicInteger[][] train_info;
	public Train_Info(int routenum,int coachnum,int seatnum,int stationnum) {
		// TODO Auto-generated constructor stub
		train_info = new AtomicInteger[routenum+1][stationnum+1];
		for(int i=1;i<=routenum;i++)
			for(int j=1;j<=stationnum;j++)
				train_info[i][j] = new AtomicInteger(coachnum*seatnum);
		train_info_lock = new Lock[routenum+1];
		for(int i=1;i<=routenum;i++)
			train_info_lock[i] = new ReentrantLock();
	}
}

class Seat_Node{//ÿ����λ����һ���ڵ㣬������λ�Ľڵ㹹
	Lock lock;
	boolean[] Station;//ÿ����λ�����¼վ����Ϣ
	long[] tid;
	String[] passenger;
	public Seat_Node(int stationnum) {
		// TODO Auto-generated constructor stub
		tid = new long[stationnum+1];
		Station = new boolean[stationnum+1];
		passenger = new String[stationnum+1];
		for(int i = 1;i<=stationnum;i++){
			tid[i] = 0;
			Station[i] = false;//false��ʾ��Ӧ���λ�û�б�����
			passenger[i] = "";
		}
		lock = new ReentrantLock();
	}
}



class Seat_Info{//��λ��,�����ܵ�n�˳�m������k����λ����Ϣ
	Seat_Node[][][] Seat_Nodes;
	public Seat_Info(int routenum,int stationnum,int coachnum,int seatnum) {
		// TODO Auto-generated constructor stub
		Seat_Nodes = new Seat_Node[routenum+1][coachnum+1][seatnum+1];
		for(int k = 1;k<=routenum;k++)
			for(int i = 1;i<=coachnum;i++)
				for(int j = 1;j<=seatnum;j++)
					Seat_Nodes[k][i][j] = new Seat_Node(stationnum);
	}
}



