package com.management.chatbot.domain;

import com.management.chatbot.Exception.AlreadyJoinedException;
import com.management.chatbot.Exception.DefaultException;
import com.management.chatbot.service.dto.ParticipationSaveRequestDto;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@Entity
@Getter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;

    // 참여자 정보
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", name = "participation")
    private List<Participation> participationList = new ArrayList<Participation>();

    // 인증 정보
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "certification")
    private List<Certification> certificationList = new ArrayList<Certification>();

    private Long savedMoney;
    private Long reward;
    private String kakaoId;
    private String kakaoName;
    private String phoneNumber;
    private Timestamp createdAt;

    @Builder
    public Member(String username, List<Participation> participationList, List<Certification> certificationList, Long savedMoney, Long reward, String kakaoId, String kakaoName, String phoneNumber, Timestamp createdAt) {
        this.username = username;
        this.participationList = participationList;
        this.certificationList = certificationList;
        this.savedMoney = savedMoney;
        this.reward = reward;
        this.kakaoId = kakaoId;
        this.kakaoName = kakaoName;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    public void addParticipation(Participation participation) {
        if (this.participationList == null) { // 신청한 챌린지가 하나도 없는 경우
            this.participationList = new ArrayList<Participation>();
        }

        // 이미 가입한 경우 예외 처리
        if (isAlreadyJoinedChallenge(participation)) {
            throw new AlreadyJoinedException(
                    "이미 진행 중인 챌린지입니다.\r" +
                            "하단의 \"챌린지 목록\"에서 \"챌린지 인증\" 버튼을 클릭해 인증 해주세요☺️");
        }

        this.participationList.add(participation);
    }

    private boolean isAlreadyJoinedChallenge(Participation newParticipation) {
        ListIterator<Participation> iter = this.participationList.listIterator();

        Timestamp now = new Timestamp(System.currentTimeMillis());

        while (iter.hasNext()) {
            Participation part = iter.next();
            if ( // 동일 챌린지가 존재 && 기존 챌린지가 아직 끝나지 않은 경우
                    part.getChallengeId().equals(newParticipation.getChallengeId())
                            && part.getEndDate() != null
                            && part.getEndDate().after(now)
            ) {
                return true;
            }
        }
        return false; // 동일 챌린지가 존재하지 않는 경우
    }

    public ParticipationSaveRequestDto addCertification(Long challengeId, CertInfo certInfo, Long savedMoney, Long reward) {

        this.savedMoney += savedMoney;
        this.reward += reward;

        if (this.certificationList == null) { // 추후에 default 값을 두고 없애도 될 듯
            this.certificationList = new ArrayList<Certification>();
        }

        ListIterator<Certification> certIter = this.certificationList.listIterator();

        while (certIter.hasNext()) {
            Certification certification = certIter.next();
            if (certification.getChallenge_id().equals(challengeId)) {
                certification.addCert(certInfo);
                return addCertificationCnt(challengeId);
            }
        }

        // 해당 챌린지 인증이 처음인 경우
        Certification newCertification = Certification.builder()
                .id(challengeId)
                .cert(new ArrayList<CertInfo>())
                .build();
        newCertification.addCert(certInfo);
        this.certificationList.add(newCertification);

        return addCertificationCnt(challengeId);
    }

    public ParticipationSaveRequestDto addCertificationCnt(Long challengeId) {
        ListIterator<Participation> partIter = this.participationList.listIterator();
        Timestamp now = new Timestamp(System.currentTimeMillis()); // 현재 시간

        while (partIter.hasNext()) {
            Participation challenge = partIter.next();
            if (challenge.getChallengeId() == challengeId
                    && challenge.getStartDate().before(now)
                    && challenge.getEndDate() != null
                    && challenge.getEndDate().after(now)) {
                return challenge.addCertificationCnt();
            }
        }
        return null;
    }

    public boolean isMaxCertification(Long challengeId, Long maxCnt) {
        if (this.certificationList == null) return false;
        ListIterator<Certification> iter = this.certificationList.listIterator();

        LocalDateTime currentDate = LocalDateTime.now();
        while (iter.hasNext()) {
            Certification certification = iter.next();
            if (certification.getChallenge_id().equals(challengeId)) {
                long cnt = 0;
                LocalDateTime dateFromTimestamp = null;
                for (CertInfo certInfo : certification.getCert()) {
                    dateFromTimestamp = certInfo.getDate().toLocalDateTime();
                    boolean isSameDate = dateFromTimestamp.toLocalDate().isEqual(currentDate.toLocalDate());
                    if (isSameDate) {
                        cnt++;
                    }
                }

                if (cnt >= maxCnt) return true;
                else if (Duration.between(dateFromTimestamp, currentDate).toHours() < 3) {
                    throw new DefaultException("동일한 챌린지의 경우 3시간 이내에는 인증을 연속으로 할 수 없습니다😓\r"
                            + "나중에 다시 인증 해주세요.");
                } else return false;
            }
        }

        return false;
    }

    public void buyGiftcard(Long price) {
        if (this.reward < price) {
            throw new DefaultException("잔액이 부족합니다.");
        } else {
            this.reward -= price;
        }
    }

    public void savePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updateReward(Long additionalReward) {
        this.reward += additionalReward;
    }

    public List<ParticipationSaveRequestDto> getParticipatingChallenges() {

        if (this.participationList == null) {
            throw new DefaultException("세이버님은 현재 참여중인 챌린지가 없습니다.\n" +
                    "하단의 \"챌린지 목록\"을 누르고 \"챌린지 종류\" 버튼을 클릭해 원하는 챌린지에 신청한 후 인증해주세요\uD83D\uDE03");
        }
        ListIterator<Participation> iter = this.participationList.listIterator();
        Timestamp now = new Timestamp(System.currentTimeMillis()); // 현재 시간

        List<ParticipationSaveRequestDto> participationSaveRequestDtoList = new ArrayList<ParticipationSaveRequestDto>();
        while (iter.hasNext()) {
            Participation challenge = iter.next();
            if (challenge.getStartDate().before(now)
                    && challenge.getEndDate() != null
                    && challenge.getEndDate().after(now)) {
                participationSaveRequestDtoList.add(new ParticipationSaveRequestDto(challenge));
            }
        }
        return participationSaveRequestDtoList;
    }
}