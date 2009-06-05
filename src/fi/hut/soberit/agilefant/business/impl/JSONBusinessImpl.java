package fi.hut.soberit.agilefant.business.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.hut.soberit.agilefant.business.JSONBusiness;
import fi.hut.soberit.agilefant.business.TeamBusiness;
import fi.hut.soberit.agilefant.business.UserBusiness;
import fi.hut.soberit.agilefant.model.Assignment;
import fi.hut.soberit.agilefant.model.Backlog;
import fi.hut.soberit.agilefant.model.ExactEstimate;
import fi.hut.soberit.agilefant.model.Iteration;
import fi.hut.soberit.agilefant.model.Project;
import fi.hut.soberit.agilefant.model.Team;
import fi.hut.soberit.agilefant.model.User;
import flexjson.JSONSerializer;

@Service("jsonBusiness")
@Transactional
public class JSONBusinessImpl implements JSONBusiness {

    @Autowired
    private UserBusiness userBusiness;
    @Autowired
    private TeamBusiness teamBusiness;

//  private BacklogItemBusiness backlogItemBusiness;
    
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public String getUserChooserJSON(int backlogId) {
//        BacklogItem bli = null;
        Backlog backlog = null;
        Collection<Integer> assignments = new ArrayList<Integer>();
        Collection<Integer> responsibles = new ArrayList<Integer>();
        Map<Integer, ExactEstimate> personalLoads = new HashMap<Integer, ExactEstimate>();

        String backlogJson = "";
        String userJson = "";
        String teamJson = "";
        String assignmentJson = "";
        String responsibleJson = "";
        String personalLoadJson = "";

        if (backlog != null) {
            Project proj = null;
            if (backlog instanceof Iteration) {
                proj = (Project)backlog.getParent();
            } else if (backlog instanceof Project) {
                proj = (Project) backlog;
            }
            if (proj != null) {
                for (Assignment ass : proj.getAssignments()) {
                    assignments.add(ass.getUser().getId());
                    personalLoads.put(ass.getUser().getId(), ass.getPersonalLoad());
                }
            }
        }
        
//        if (!(backlogItemId == 0 && backlogId == 0)) {
//            try {
//                bli = backlogItemBusiness.getBacklogItem(backlogItemId);
//                if (backlogId > 0) {
//                    backlog = backlogBusiness.getBacklog(backlogId);
//                }
//                else if (bli != null) {
//                    backlog = bli.getBacklog();
//                }
//            } catch (ObjectNotFoundException onfe) {
//            }
//    
//            /*
//             * Get the assignments.
//             */
//            
//            /*
//             * Get the bli's responsibles.
//             */
//            if (bli != null) {
//                for (User user : bli.getResponsibles()) {
//                    responsibles.add(user.getId());
//                }
//            }
//        }
        /*
         * Get all teams and users as json
         */
        List<User> users = new ArrayList<User>(userBusiness.retrieveAll());
        Collections.sort(users, new PropertyComparator("fullName", false, true));
        List<Team> teams = new ArrayList<Team>(teamBusiness.retrieveAll());
        Collections.sort(teams, new PropertyComparator("name", false, true));
        
        userJson = new JSONSerializer().include("id").include("fullName")
                .include("initials").include("enabled").exclude("*").serialize(users);
        teamJson = new JSONSerializer().include("users.id").exclude("users.*")
                .serialize(teams);

        /* Get the other jsons */
        assignmentJson = new JSONSerializer().serialize(assignments);
        responsibleJson = new JSONSerializer().include("id").exclude("*")
                .serialize(responsibles);
        personalLoadJson = new JSONSerializer().serialize(personalLoads);
        backlogJson = new JSONSerializer().include("id").include("defaultOverhead").exclude("*").serialize(backlog);

        return "{users:" + userJson + ",teams:" + teamJson + ",assignments:"
                + assignmentJson + ",responsibles:" + responsibleJson + "," +
                "personalLoads:" + personalLoadJson + "," +
                "backlog:" + backlogJson + "}";
    }
    
    /** {@inheritDoc}} */
    public String objectToJSON(Object object) {
        return new JSONSerializer().serialize(object);
    }

    /*
     * AUTOGENERATED LIST OF GETTERS AND SETTERS
     */
    public void setUserBusiness(UserBusiness userBusiness) {
        this.userBusiness = userBusiness;
    }

//    public void setBacklogItemBusiness(BacklogItemBusiness backlogItemBusiness) {
//        this.backlogItemBusiness = backlogItemBusiness;
//    }

    public void setTeamBusiness(TeamBusiness teamBusiness) {
        this.teamBusiness = teamBusiness;
    }
}
