import java.net.*;
import java.io.*;
import java.util.Scanner;

public class ChatClient {

	public static void main(String[] args) {
		if(args.length != 0){	//옵션개수검사. 옵션이 2개가 아니면, 밑에 메세지 출력후 프로그램종료
			//System.out.println("Usage : java ChatClient <username> <server-ip>");	//메세지출력
			System.out.println("Usage : java ChatClient");	//메세지출력
			System.exit(1); //프로그램 종료
			//username	==> args[0]
			//server-ip ==> args[1]
		}

		//ChatClient_new chatClient = new ChatClient_new(args[1], args[0]);
		ChatClient_new chatClient = new ChatClient_new();
		chatClient.start();
	}
}

class ChatClient_new {
	private String serverip="";
	private String username = "";
			
	Socket sock = null; 
	BufferedReader br = null; //BufferedReader br 선언
	BufferedReader keyboard = null;
	PrintWriter pw = null;	//PrintWriter pw 선언
	boolean endflag = false;	//boolean변수 endflag를 false로 초기화하여 선언(종료를 확인할 변수)
	Scanner kb = new Scanner(System.in); 
	
	public ChatClient_new() { //constructor
		System.out.print("your name >> ");
		this.username = kb.nextLine();
		System.out.print("server ip >> ");
		this.serverip = kb.nextLine();
	}	
	
	public void start() {
		setsocket();
			
		InputThread it = new InputThread(sock, br);
		it.start();
			
		check_console();
			
		closes();
	}
	
	public void setsocket() {
		try {
			sock = new Socket(serverip, 10001); //소켓객체 생성하여 10001번 포트 서버에 연결시도
			pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));//소켓의 출력 스트림 얻기
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));//소켓의 입력 스트림 얻기
			keyboard = new BufferedReader(new InputStreamReader(System.in));
			//콘솔창에서 받을 입력을 위한 스트림 객체 생성
			
			pw.println(username); //사용자의 아이디 전송
			pw.flush(); //또다른 입력을 위해 스트림 객체 내부의 버퍼를 비운다.
			
		}catch(Exception ex){ //소켓이나 콘솔입력오류가 발생시
			if(!endflag) //콘솔입력이 종료되지 않았으면
				System.out.println(ex); //에러표시
		}
	}
	
	public void check_console() {
		try {
		String line = null; //콘솔 입력값이 있는가 체크하기위한 변수 선언
		while((line = keyboard.readLine()) != null){ //콘솔에서 한줄을 입력받음
			pw.println(line); //소켓 쓰기 스트림으로 콘솔에서 한줄입력받은 String을 전송
			pw.flush(); //또다른 쓰기를 위해서 스트림버퍼를 비우기
			if(line.equals("/quit")){	//받은 입력이 '/quit'이면!
				endflag = true; //boolean변수 endflag를 true로 바꾸고 종료
				break;
			}
		}
		System.out.println("Connection closed.");
		
		}catch(Exception ex){ //소켓이나 콘솔입력오류가 발생시
			if(!endflag) //콘솔입력이 종료되지 않았으면
				System.out.println(ex); //에러표시
		}
	}
	
	public void closes() {
		try{
			if(pw != null) //소켓 출력스트림이 정상적으로 생성되었으면, 
				pw.close(); //쓰기스트림 닫기.
		}catch(Exception ex){}
		try{
			if(br != null) //소켓 읽기스트림이 정상적으로 생성되었으면, 
				br.close();  //  읽기스트림 닫기.
		}catch(Exception ex){}
		try{
			if(sock != null) //소켓이 정상적으로 서버와 연결되었으면,
				sock.close(); //소켓을 닫기
		}catch(Exception ex){}
	}
}

//동작하고 있는 프로그램을 프로세스(Process)라고 한다. 
//보통 한 개의 프로세스는 한 가지의 일을 하지만, 
//쓰레드를 이용하면 한 프로세스 내에서 두 가지 또는 그 이상의 일을 동시에 할 수 있게 된다.
class InputThread extends Thread{ 
	private Socket sock = null; //소켓 'sock'선언
	private BufferedReader br = null; //읽기 스트림 선언
	public InputThread(Socket sock, BufferedReader br){
		this.sock = sock; //소켓 객체 설정
		this.br = br; //읽기스트림 객체 생성
	}
	public void run(){ //메인에서 start 메소드 실행 시 이 run 메소드가 자동으로 수행
		try{
			String line = null; //읽기스트림에서 String을 가져오기 위한 변수
			while((line = br.readLine()) != null){	//스트림에서 String을 읽기
				System.out.println(line);	//스트림에서 읽어온 String을 콘솔창에 출력
			}
		}catch(Exception ex){ 
		}finally{
			try{
				if(br != null) //읽기스트림 객체가 제대로 설정되었고, 설정된 스트림 객체가 제대로 연결된 스트림 객체라면
					br.close(); // 읽기 스트림 닫기	
			}catch(Exception ex){}
			try{
				if(sock != null) //소켓 객체가 제대로 설정되었고, 설정된 소켓 객체가 제대로 연결되어 생성된 소켓 객체라면
					sock.close(); //소켓 양방향 스트림 닫기	
			}catch(Exception ex){}
		}
	} // InputThread
}
