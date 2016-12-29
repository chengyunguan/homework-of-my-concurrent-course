package ticketingsystem;

public interface TicketingSystem {
	Ticket buyTicket(String passenger,int route,int depature,int arrival);
	int inquiry(int route,int depature,int arrival);
	boolean refundTicket(Ticket ticket);
}

class Ticket{
	long tid;
	String passenger;
	int route;
	int coach;
	int seat;
	int departure;
	int arrival;
	
}