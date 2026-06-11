package com.ai.ai_research_agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ai.ai_research_agent.mapper")
public class AiResearchAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiResearchAgentApplication.class, args);
        System.out.println("✅✅✅✅✅✅------------     Java后端启动成功！---------     ✅✅✅✅✅✅");
	}

}
