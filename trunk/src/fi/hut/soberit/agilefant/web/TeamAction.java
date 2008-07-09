package fi.hut.soberit.agilefant.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opensymphony.xwork.Action;
import com.opensymphony.xwork.ActionSupport;

import fi.hut.soberit.agilefant.db.TeamDAO;
import fi.hut.soberit.agilefant.db.UserDAO;
import fi.hut.soberit.agilefant.model.Team;
import fi.hut.soberit.agilefant.model.User;

public class TeamAction extends ActionSupport implements CRUDAction {

    private static final long serialVersionUID = -3334278151418035144L;

    private int teamId;

    private Team team;

    private TeamDAO teamDAO;

    private UserDAO userDAO;

    private Map<Integer, String> userIds = new HashMap<Integer, String>();
    
    private List<Team> teamList = new ArrayList<Team>();

    /**
     * Create a new team.
     */
    public String create() {
        teamId = 0;
        team = new Team();
        team.setUsers(new ArrayList<User>());
        return Action.SUCCESS;
    }
    
    public String list() {
        teamList.addAll(teamDAO.getAll());
        return Action.SUCCESS;
    }

    /**
     * Delete an existing team.
     */
    public String delete() {
        teamDAO.remove(teamId);
        return Action.SUCCESS;
    }

    /**
     * Edit a team.
     */
    public String edit() {
        team = teamDAO.get(teamId);
        if (team == null) {
            super.addActionError("Team not found!");
            return Action.ERROR;
        }
        return Action.SUCCESS;
    }

    /**
     * Store the team.
     */
    public String store() {
        Team storable = new Team();
        if (teamId > 0) {
            storable = teamDAO.get(teamId);
            if (storable == null) {
                super.addActionError("Team not found!");
                return Action.ERROR;
            }
        }

        // Fill the storable element
        fillStorable(storable);

        // Check, if action has errors.
        if (super.hasActionErrors()) {
            return Action.ERROR;
        }
        teamDAO.store(storable);
        return Action.SUCCESS;
    }

    protected void fillStorable(Team storable) {
        storable.setDescription(team.getDescription());

        // Check that the name is valid
        if (team.getName() == null || team.getName().length() == 0 ||
                team.getName().trim().compareTo("") == 0) {
            super.addActionError("The team name can't be empty.");
            return;
        }
        storable.setName(team.getName());

        // Set the users
        Collection<User> users = new ArrayList<User>();

        for (Integer uid : userIds.keySet()) {
            users.add(userDAO.get(uid));
        }

        storable.setUsers(users);
    }

    /*
     * List of autogenerated setters and getters
     */

    /**
     * @return the teamId
     */
    public int getTeamId() {
        return teamId;
    }

    /**
     * @param teamId
     *                the teamId to set
     */
    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    /**
     * @return the team
     */
    public Team getTeam() {
        return team;
    }

    /**
     * @param team
     *                the team to set
     */
    public void setTeam(Team team) {
        this.team = team;
    }

    /**
     * @return the teamDAO
     */
    public TeamDAO getTeamDAO() {
        return teamDAO;
    }

    /**
     * @param teamDAO
     *                the teamDAO to set
     */
    public void setTeamDAO(TeamDAO teamDAO) {
        this.teamDAO = teamDAO;
    }

    public UserDAO getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public Map<Integer, String> getUserIds() {
        return userIds;
    }

    public void setUserIds(Map<Integer, String> userIds) {
        this.userIds = userIds;
    }

    public List<Team> getTeamList() {
        return teamList;
    }

    public void setTeamList(List<Team> teamList) {
        this.teamList = teamList;
    }
    
}
