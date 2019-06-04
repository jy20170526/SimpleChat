import java.net.*;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatServer {

	public static void main(String[] args) {
		try{
			ServerSocket server = new ServerSocket(10001); // 10001번 포트에 바인딩된 ServerSocket 생성
			//ServerSocket => Client에서 들어오는 요청을 기다리는 Server Socket을 구현하는 클래스. 
			System.out.println("Waiting connection...");
			HashMap hm = new HashMap(); //서버에서 클라이언트들의 쓰기 스트림 관리하는 객체로 hashmap을 사용.
			//클라이언트에서 받아온 아이디를 key로 만들고, 각각의 클라이언트 소켓의 쓰기 스트림 객체를 불러오고 저장.

			while(true){ //무한히 클라이언트 받기
				Socket sock = server.accept(); 
				//accept => 클라이언트의 접속을 기다림(접속이 올때까지)
    		//접속이 올 경우 연결된 클라이언트의 소켓을 생성
				ChatThread chatthread = new ChatThread(sock, hm); //클라이언트 대응 쓰레드 객체 생성
				chatthread.start(); //연결된 클라이언트를 생성자로 하여 스레드 시작
			} // while
		}catch(Exception e){ //소켓 클라이언트를 받아들이는 도중 오류 or 서버 바인딩 과정에서 오류가 발생시	
			System.out.println(e); //에러출력	
		}
	} // main
}
//main ==> 서버를 바인딩 + 클라이언트를 무한대로 받아서 관련된 서브쓰레드에 넘겨 처리함	

class ChatThread extends Thread{
	private Socket sock; //클라이언트 소켓 객체변수 'sock' 선언
	private String id; //클라이언트로부터 받아온 id
	private BufferedReader br; //클라이언트와의 통신에서 서버측에서 읽는 스트림 객체 선언	
	private HashMap hm; //아이디와 해당하 아이디의 소켓 쓰기스트림 객체를 관리하는 메인 쓰레드의 객체에서 간접 참조해주는 변수
	private boolean initFlag = false;
	private String[] badwords = {"ugly", "fat", "angry", "mad", "fight", "stupid"};
	
	SimpleDateFormat df = new SimpleDateFormat("[a h:mm] ");
	
	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock; //연결돤 클라이언트 소켓을 멤버변수에 저장	
		this.hm = hm; //아이디와 비밀번호 관리 객체를 간접참조하기 위해 설정
		try{
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			//클라이언트소켓에서 받아온 스트림을 서버입장에서 읽기 스트림형식으로서 스트림객체 생성
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			//소켓 통신을 위한 소켓 읽기 스트림을 가지고 스트림 객체 생성
			id = br.readLine(); //id를 클라이언트 측에서 받아옴
			broadcast(id + " entered.");
			//broadcast() 메소드는 문자열을 인자로 전달 받은 후, 
			//HashMap에 저장된 PrintWriter를 하나씩 얻어 사용. 
			//HashMap에는 접속된 모든 클라이언트의 PrintWriter가 있기 때문에 
			//HashMap으로부터 PrintWriter 객체를 얻어와서 println() 메소드로 문자열을 출력한다는 것은 
			//접속된 모든 클라이언트에게 문자열을 전송하는 효과를 발생
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){ 
				hm.put(this.id, pw); //클라이언트 관리객체에 id를 key로서 해당하는 클라이언트의 쓰기스트림 객체를 저장
			}
			//synchronized ==> 자바 코드에서 동기화 영역은 synchronizred 키워드로 표시. 
			//동기화는 객체에 대한 동기화로 이루어지는데, 
			//같은 객체에 대한 모든 동기화 블록은 한 시점에 오직 한 쓰레드만이 블록 안으로 접근하도록함 
			//블록에 접근을 시도하는 다른 쓰레드들은 블록 안의 쓰레드가 실행을 마치고 블록을 벗어날 때까지 
			//블록(blocked) 상태가 된다.
			//synchronized 블록 안에서 HashMap의 put() 메소드를 사용한 이유는 
			//여러 스레드가 HashMap을 공유하기 때문인데, HashMap에 있는 자료를 
			//삭제하거나, 수정하거나, 읽어오는 부분이 동시에 일어날 수 있기 때문
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	//run() 메소드는 실제로 스레드의 동작을 정의하는 가장 중요한 메소드
	//소켓을 통해서 한 줄씩 읽어 들인 후 읽어 들인 문자열이 
	//"/quit"일 경우에는 클라이언트가 종료 메시지를 보낸 것으로 판단해서 while문에서 빠져나감. 
	//만약, "/to"로 시작하는 문자열을 전송했다면, 전체에게 보내는 메시지가 아니라 
	//특정 아이디의 클라이언트에게 보내는 문자열로 판단하게 함(귓속말 기능)
	//이를 제외한 나머지 문자열은 앞서 설명한 broadcast() 메소드를 이용해서, 접속한 모든 클라이언트에게 문자열을 전송
	public void run(){
		try{
			String line = null; //읽기 스트림에서 String을 가져오기위해서 쓰임 	
			while((line = br.readLine()) != null){ //한문장을 읽어서 
				if(line.equals("/quit")) //읽은 스트링이 '/quit'이면
					break; //while문 종료 + 읽기 종료
				if(line.equals("/userlist")) { //현재 접속한 사용자들의 id 리스트와 총 사용자 수를 보여줌 
					send_userlist(line); 
				}else if(line.indexOf("/to ") == 0){ //문자열이 ''/to'로 시작하면 	
					sendmsg(line);	//지정한 사용자로 메세지를 보내는 메소드를 실행
				}else
					broadcast(id + " : " + line); //지정된 아이디로 메세지를 보낼것이 아니면 
					//지정된 아이디로 받아온 메세지를 모든 콘솔에 출력하는 메소드 실행
			}
		}catch(Exception ex){ //에러시
			System.out.println(ex); //에러출력 	
		}finally{ //에러가 나든 안나든 실행 	
			synchronized(hm){ //쓰레드 동기화 	
				hm.remove(id); //클라이언트 관리객체에서 해당하는 아이디를 포함한 데이터 삭제
			}
			broadcast(id + " exited.");
			try{
				if(sock != null) //소켓에 에러가 없었다면,
					sock.close(); //소켓을 닫기. 	
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg){ //특정 클라이언트에게 메세지를 전송하는 메소드	
		boolean ok = checkBadwords(msg); //금기어가 포함됬는지 확인(없으면 false)
		if(ok == false) {
			int start = msg.indexOf(" ") +1; //보낼 아이디를 추출해낼 초기위치 검색
			int end = msg.indexOf(" ", start);//보낼 아이디를 추출해낼 마지막위치 검색	
			if(end != -1){
				String to = msg.substring(start, end); //상대의 아이디 추출
				String msg2 = msg.substring(end+1); //보낼 메세지 추출
				Object obj = hm.get(to); //추출해낸 아이디를 아이디관리객체에서 불러옴
				if(obj != null){ //해당하는 아이디가 존재하면
					PrintWriter pw = (PrintWriter)obj; //불러온 객체를 강제 형변환
					Date date1 = new Date();
				    String today1 = df.format(date1);
					pw.print(today1); 
					pw.println(id + " whisphered. : " + msg2); //상대 객체의 쓰기 객체에 메세지를 전송
					pw.flush(); //버퍼 비우기 	
				} // if
			}
		}
	} // sendmsg
	public void broadcast(String msg){ //접속된 모든 클라이언트에게 현재 서브 쓰레드에서 처리하고 있는 소켓의 내용을 전송하고 출력
		boolean ok = checkBadwords(msg); //금기어가 포함됬는지 확인(없으면 false)
		if(ok == false) {
			synchronized(hm){ //쓰레드 동기화 	
				Set set = hm.keySet();
				Iterator iter = set.iterator(); //한방향 접근객체로서 데이터를 불러옴
				while(iter.hasNext()){ //접근할 데이터가 아직 존재할 경우
					Object obj2 = iter.next();
					if(id == obj2) {
						continue;
					}
					PrintWriter pw = (PrintWriter)(hm.get(obj2)); //접근할 데이터를 가져와 강제 형변환
					Date date2 = new Date();
					String today2 = df.format(date2);
					pw.print(today2); 
					pw.println(msg); //가져온 접근 객체에 메세지를 전송
					pw.flush(); //버퍼비우기
				}
			}
		}
	} // broadcast
	
	//보내려는 message에 금기어가 포함됬는지 확인하는 함수 
	public boolean checkBadwords(String msg) {
		boolean result = false;
		for(int i=0; i<6; i++) {
			if(msg.contains(badwords[i])) {
				Object obj = hm.get(id);
				PrintWriter pw = (PrintWriter)obj;	
				pw.println("|WARNING|  "+ badwords[i]+ " is forbidden word!!! Cannot send it!");
				result = true;
				pw.flush();
			}
		}
		return result;
	}
	
	public void send_userlist(String msg) {
		//현재 접속한 사용자들의 id 리스트
		Object obj = hm.get(id);
		Set set = hm.keySet();
		Iterator iterator = set.iterator();
		if(obj != null){
			PrintWriter pw = (PrintWriter)obj;	
			pw.println("총 사용자 수 : " + hm.size()+"명");
			while(iterator.hasNext()){
				String key = (String)iterator.next();
				pw.println(key);
			}
			pw.flush();
		}
	}
}
//ChatThread ==> 먼저 클라이언트쪽에서 전송된 String을 가져옴.
//스트링이 /to로 시작하고 /to 형식이면 현재 서브쓰레드의 인자로 전달된 id소켓에서 
//(지정한id)가 접속되있고 소켓의 쓰기 스트림으로 (지정한id)에게만 전달. 
//그렇지 않으면 접속된 모든 클라이언트에게 받아온 스트링을 전송
