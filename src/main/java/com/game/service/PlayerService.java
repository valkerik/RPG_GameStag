package com.game.service;

import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.exceptions.BadRequestException;
import com.game.exceptions.NotFoundException;
import com.game.repository.PlayerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class PlayerService {
   private static final int NAME_MAXLENGTH = 12;
    private static final int TITLE_MAXLENGTH = 12;
    private static final int MAX_EXPERIENCE = 10000000;
    private static final int MIN_BIRTHDAY = 2000;
    private static final int MAX_BIRTHDAY = 3000;
    
    private PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Integer currentLVLCalculate(Integer exp){
        return (((int)Math.sqrt(2500 + 200*exp-50))-50)/100;
    }

    public Integer experienceForNextLevel(Integer exp, Integer lvl){
        return 50*(lvl+1)*(lvl+2)-exp;
    }

    public Page<Player> findAllPlayers (Specification<Player> specification, Pageable pageable){
        return playerRepository.findAll(specification, pageable);
    }
    public Long getCountPlayers(Specification<Player> specification){
        return playerRepository.count(specification);
    }

    public Player createPlayer(Player player){
        checkName(player.getName());
        checkTitle(player.getTitle());
        checkRace(player.getRace());
        checkProfession(player.getProfession());
        checkExperiense(player.getExperience());
        checkBirthday(player.getBirthday());

        if(player.getBanned() == null){
            player.setBanned(false);
        }
        player.setLevel(currentLVLCalculate(player.getExperience()));
        player.setUntilNextLevel(experienceForNextLevel(player.getExperience(), player.getLevel()));

        return playerRepository.saveAndFlush(player);
    }

    public Player updatePlayer (Long id, Player player){
        Player updatePlayer = getPlayerByID(id);

        if(player.getName() != null){
            checkName(player.getName());
            updatePlayer.setName(player.getName());
        }

        if(player.getTitle()!= null){
            checkTitle(player.getTitle());
            updatePlayer.setTitle(player.getTitle());
        }

        if(player.getRace()!= null){
            checkRace(player.getRace());
            updatePlayer.setRace(player.getRace());
        }

        if(player.getProfession() != null){
            checkProfession(player.getProfession());
            updatePlayer.setProfession(player.getProfession());
        }

        if(player.getExperience() != null){
            checkExperiense(player.getExperience());
            updatePlayer.setExperience(player.getExperience());
        }

        updatePlayer.setLevel(currentLVLCalculate(updatePlayer.getExperience()));
        updatePlayer.setUntilNextLevel(experienceForNextLevel(updatePlayer.getExperience(), updatePlayer.getLevel()));

        if(player.getBirthday() != null){
            checkBirthday(player.getBirthday());
            updatePlayer.setBirthday(player.getBirthday());
        }

        if(player.getBanned() != null){
            updatePlayer.setBanned(player.getBanned());
        }


        return playerRepository.save(updatePlayer);
    }

    public Player deletePlayer (Long id){
        Player player = getPlayerByID(id);
        playerRepository.delete(player);
        return player;
    }

    public Player getPlayerByID(Long id){
        checkId(id);
        return playerRepository.findById(id).orElseThrow(()
                -> new NotFoundException("Player Not Found!"));
    }

    public void checkId(Long id){
        if(id<=0){
            throw new BadRequestException("Invalid ID!");
        }
    }

    public void checkName(String name){
        if(name == null || name.isEmpty() || name.length() > NAME_MAXLENGTH){
            throw new BadRequestException("Invalid Name!");
        }
    }

    public void checkTitle (String title){
        if(title==null || title.isEmpty() || title.length()> TITLE_MAXLENGTH){
            throw new BadRequestException("Invalid Title!");
        }
    }

    public void checkRace(Race race){
        if(race==null){
            throw new BadRequestException("Invalid Race!");
        }
    }

    public void checkProfession(Profession profession){
        if(profession==null){
            throw new BadRequestException("Invalid Profession!");
        }
    }

    public void checkBirthday(Date birthday){
        if(birthday == null){
            throw new BadRequestException("Invalid Birthday!");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(birthday.getTime());
        if(calendar.get(Calendar.YEAR)< MIN_BIRTHDAY || calendar.get(Calendar.YEAR) > MAX_BIRTHDAY ){
            throw new BadRequestException("Is out of bounds!");
        }
    }

    public void checkExperiense(Integer experiense){
        if(experiense<0 || experiense > MAX_EXPERIENCE || experiense == null){
            throw new BadRequestException("Invalid Experiense!");
        }
    }


    public Specification<Player> filterByName(String name) {
        return (root,query,cb)->name==null?null:cb.like(root.get("name"),"%"+name+"%");
    }

    public Specification<Player> filterByTitle(String title) {
        return (root,query,cb)->title==null?null:cb.like(root.get("title"),"%"+title+"%");
    }

    public Specification<Player> filterByRace(Race race) {
        return (root,query,cb)->race==null?null:cb.equal(root.get("race"),race);
    }

    public Specification<Player> filterByProfession(Profession profession) {
        return (root,query,cb)->profession==null?null:cb.equal(root.get("profession"),profession);
    }

    public Specification<Player> filterByExperience(Integer min,Integer max) {
        return (root,query,cb)->{
            if (min==null && max==null) return null;
            if (min==null) return cb.lessThanOrEqualTo(root.get("experience"), max);
            if (max==null) return cb.greaterThanOrEqualTo(root.get("experience"), min);
            return cb.between(root.get("experience"), min, max);
        };
    }

    public Specification<Player> filterByLevel(Integer min,Integer max) {
        return (root,query,cb)->{
            if (min==null && max==null) return null;
            if (min==null) return cb.lessThanOrEqualTo(root.get("level"), max);
            if (max==null) return cb.greaterThanOrEqualTo(root.get("level"), min);
            return cb.between(root.get("level"), min, max);
        };
    }

    public Specification<Player> filterByBirthday(Long after, Long before) {
        return (root,query,cb)->{
            if (after==null && before==null) return null;
            if (after==null) return cb.lessThanOrEqualTo(root.get("birthday"), new Date(before));
            if (before==null) return cb.greaterThanOrEqualTo(root.get("birthday"), new Date(after));
            return cb.between(root.get("birthday"), new Date(after), new Date(before));
        };
    }

    public Specification<Player> filterByBanned(Boolean isBanned) {
        return (root,query,cb)->{
            if (isBanned==null) return null;
            if (isBanned) return cb.isTrue(root.get("banned"));
            return cb.isFalse(root.get("banned"));
        };
    }

}
