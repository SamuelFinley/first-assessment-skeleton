package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Server implements Runnable {
	private Logger log = LoggerFactory.getLogger(Server.class);
	
	private int port;
	private ExecutorService executor;
	
	
	
	public Server(int port, ExecutorService executor) {
		super();
		this.port = port;
		this.executor = executor;
		
	}

	private static HashMap<String, Socket> online = new HashMap<String, Socket>();

	public HashMap<String, Socket> clients(Message msg, Socket sock) {
		if (!Server.online.containsKey(msg.getUsername())) {
			LocalTime clk = LocalTime.now();
			String time = clk.toString();
			String[] times = time.split(Pattern.quote("."), 0);
			String hammerTime = times[0];
			msg.setTime(hammerTime);
			for (String conns : Server.online.keySet()) {
				Socket conn = Server.online.get(conns);
				try {
					ObjectMapper mapper = new ObjectMapper();
					PrintWriter newWriter = new PrintWriter(new OutputStreamWriter(conn.getOutputStream()));
					msg.setCommand("connect");
					msg.setUsername(msg.getUsername());
					String sendAll = mapper.writeValueAsString(msg);
					newWriter.write(sendAll);
					newWriter.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			Server.online.put((String) msg.getUsername(), sock);
		}
		return Server.online;
	}
	
	public static Map<String, Socket> getClients() {
		return online;
	}
	

	public void run() {
		log.info("server started");
		ServerSocket ss;
		try {
			ss = new ServerSocket(this.port);
			while (true) {
				Socket socket = ss.accept();
				ObjectMapper mapper = new ObjectMapper();
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String raw = reader.readLine();
				Message message = mapper.readValue(raw, Message.class);
				clients(message, socket);
				ClientHandler handler = new ClientHandler(socket);
				executor.execute(handler);
			}
		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
	}

}
