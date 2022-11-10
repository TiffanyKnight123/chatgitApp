package com.app.chatGit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CqlSessionBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Arrays;

@SpringBootApplication
public class ChatGitApp {
	@Autowired
	FolderRepository folderRepository;

	@Autowired
	EmailService emailService;


	public static void main(String[] args) {
		SpringApplication.run(ChatGitApp.class, args);
	}

	@Bean
	public CqlSessionBuilderCustomizer sessionBuilderCustomizer(DataStaxAstraProperties astraProperties){
		Path bundle = astraProperties.getSecureConnectBundle().toPath();
		return builder -> builder.withCloudSecureConnectBundle(bundle);
	}

	@PostConstruct
	public void init() {
		folderRepository.save(new Folder("awasthi-shashwat", "Inbox", "blue"));
		folderRepository.save(new Folder("awasthi-shashwat", "Sent", "green"));
		folderRepository.save(new Folder("awasthi-shashwat", "Important", "yellow"));

		for(int i=0;i<10;i++){
			emailService.sendEmail(
					"shashwat-awasthi",
					Arrays.asList("shashwat-awasthi"),
					"hello" + i,
					"body");
		}

	}

}
