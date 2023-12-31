package com.management.chatbot.controller;

import com.management.chatbot.service.ChallengeService;
import com.management.chatbot.service.dto.ChallengeSaveRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/challenge")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    @PostMapping("/save") // 챌린지 저장
    public Long save(@RequestBody ChallengeSaveRequestDto challengeSaveRequestDto) {
        System.out.println(challengeSaveRequestDto.getTitle());
        return challengeService.save(challengeSaveRequestDto);
    }
}