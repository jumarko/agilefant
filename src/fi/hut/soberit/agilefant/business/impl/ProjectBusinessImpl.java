package fi.hut.soberit.agilefant.business.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.hut.soberit.agilefant.business.ProductBusiness;
import fi.hut.soberit.agilefant.business.ProjectBusiness;
import fi.hut.soberit.agilefant.business.RankUnderDelegate;
import fi.hut.soberit.agilefant.business.RankingBusiness;
import fi.hut.soberit.agilefant.business.SettingBusiness;
import fi.hut.soberit.agilefant.business.TransferObjectBusiness;
import fi.hut.soberit.agilefant.db.AssignmentDAO;
import fi.hut.soberit.agilefant.db.BacklogDAO;
import fi.hut.soberit.agilefant.db.ProjectDAO;
import fi.hut.soberit.agilefant.db.StoryHierarchyDAO;
import fi.hut.soberit.agilefant.db.UserDAO;
import fi.hut.soberit.agilefant.exception.ObjectNotFoundException;
import fi.hut.soberit.agilefant.model.Assignment;
import fi.hut.soberit.agilefant.model.Backlog;
import fi.hut.soberit.agilefant.model.Iteration;
import fi.hut.soberit.agilefant.model.Product;
import fi.hut.soberit.agilefant.model.Project;
import fi.hut.soberit.agilefant.model.Rankable;
import fi.hut.soberit.agilefant.model.Story;
import fi.hut.soberit.agilefant.model.User;
import fi.hut.soberit.agilefant.transfer.IterationTO;
import fi.hut.soberit.agilefant.transfer.ProjectMetrics;
import fi.hut.soberit.agilefant.transfer.ProjectTO;

@Service("projectBusiness")
@Transactional
public class ProjectBusinessImpl extends GenericBusinessImpl<Project> implements
        ProjectBusiness {

    private ProjectDAO projectDAO;
    private BacklogDAO backlogDAO;
    private ProductBusiness productBusiness;
    private StoryHierarchyDAO storyHierarchyDAO;
    private AssignmentDAO assignmentDAO;
    private UserDAO userDAO;
    
    private TransferObjectBusiness transferObjectBusiness;
    private RankingBusiness rankingBusiness;
    
    @Autowired
    private SettingBusiness settingBusiness;

    public ProjectBusinessImpl() {
        super(Project.class);
    }
    
    @Autowired
    public void setProjectDAO(ProjectDAO projectDAO) {
        this.genericDAO = projectDAO;
        this.projectDAO = projectDAO;
    }

    @Autowired
    public void setBacklogDAO(BacklogDAO backlogDAO) {
        this.backlogDAO = backlogDAO;
    }    
    
    @Autowired
    public void setRankingBusiness(RankingBusiness rankingBusiness) {
        this.rankingBusiness = rankingBusiness;
    }
    
    @Autowired
    public void setProductBusiness(ProductBusiness productBusiness) {
        this.productBusiness = productBusiness;
    }
    
    @Autowired
    public void setTransferObjectBusiness(TransferObjectBusiness transferObjectBusiness) {
        this.transferObjectBusiness = transferObjectBusiness;
    }
    
    @Autowired
    public void setStoryHierarchyDAO(StoryHierarchyDAO storyHierarchyDAO) {
        this.storyHierarchyDAO = storyHierarchyDAO;
    }
    
    @Autowired
    public void setAssignmentDAO(AssignmentDAO assignmentDAO) {
        this.assignmentDAO = assignmentDAO;
    }
    
    @Autowired
    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }
    
    @Transactional
    public void unrankProject(int projectId) {
        Project project = projectDAO.get(projectId);
        project.setRank(0);
    }
    
    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    public ProjectMetrics getProjectMetrics(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project must be supplied");
        }
        ProjectMetrics metrics = new ProjectMetrics();
        metrics.setStoryPoints(
                backlogDAO.calculateStoryPointSumIncludeChildBacklogs(project.getId()));
        return metrics;
    }
    
    /** {@inheritDoc} */
    public Project store(int projectId,
            Integer productId, Project project, Set<Integer> assigneeIds) throws ObjectNotFoundException,
            IllegalArgumentException {

        Project persistable = new Project();
        if (projectId > 0) {
            persistable = this.retrieve(projectId);       
        } 
        validateProjectData(project, projectId, productId);
        if(productId != null ){
            Product product = this.productBusiness.retrieve(productId);
            persistable.setParent(product);
        }
        setAssignees(persistable, assigneeIds);
        
        persistable.setName(project.getName());
        persistable.setStartDate(project.getStartDate());
        persistable.setEndDate(project.getEndDate());
        persistable.setProjectType(project.getProjectType());
        persistable.setDescription(project.getDescription());
        persistable.setStatus(project.getStatus());
        persistable.setBacklogSize(project.getBacklogSize());        
        Project stored = persistProject(persistable);
        
        return stored;
    }
    
    private void setAssignees(Project project, Set<Integer> assigneeIds) {
        if (assigneeIds != null) {
            Map<Integer, Assignment> userIdsAndAssigments = new HashMap<Integer, Assignment>();
            for (Assignment assignment : project.getAssignments()) {
                userIdsAndAssigments.put(assignment.getUser().getId(), assignment);
            }
            for (Map.Entry<Integer, Assignment> existingValue : userIdsAndAssigments.entrySet()) {
                if (!assigneeIds.contains(existingValue.getKey())) {
                    project.getAssignments().remove(existingValue.getValue());
                    assignmentDAO.remove(existingValue.getValue());
                }
            }
            for (Integer assigneeId : assigneeIds) {
                if (!userIdsAndAssigments.containsKey(assigneeId)) {
                    User user = userDAO.get(assigneeId);
                    Assignment assignment = new Assignment(user, project);
                    project.getAssignments().add(assignment);
                    assignmentDAO.create(assignment);
                }
            }
        }
    }
    /**
     * Persists a given project.
     * <p>
     * Decides whether to use <code>store</code> or <code>create</code>.
     * @return the persisted project
     */
    private Project persistProject(Project project) {
        if (project.getId() > 0) {
            this.store(project);
            return project;
        }
        else {
            int newId = this.create(project);
            return this.retrieve(newId);
        }
    }
    
    /**
     * Validates the given project's data.
     * <p>
     * Currently checks start and end date. 
     */
    private static void validateProjectData(Project project, int projectId, Integer productId)
        throws IllegalArgumentException {
        if (project.getStartDate().isAfter(project.getEndDate())) {
            throw new IllegalArgumentException("Project start date after end date.");
        }
        if(projectId == 0 && productId == null) {
            throw new IllegalArgumentException("New project must have a parent product");
        }
    }


    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    public Collection<User> getAssignedUsers(Project project) {
        return projectDAO.getAssignedUsers(project);
    }


    /** {@inheritDoc} */
    @Transactional(readOnly = true)
    public ProjectTO getProjectData(int projectId) {
        Project original = this.retrieve(projectId);
        ProjectTO project = transferObjectBusiness.constructProjectTO(original);
        project.setChildren(new ArrayList<Backlog>());
        for (Backlog backlog : original.getChildren()) {
            IterationTO iter = new IterationTO((Iteration)backlog);
            iter.setScheduleStatus(transferObjectBusiness.getBacklogScheduleStatus(backlog));
            project.getChildren().add(iter);
        }
        return project;
    }

    public List<Story> retrievetRootStories(Project project) {
        return this.storyHierarchyDAO.retrieveProjectRootStories(project);
    }

    @Transactional
    public Project rankUnderProject(int projectId, int rankUnderId) {
        Project project = projectDAO.get(projectId);
        Project rankUnder = projectDAO.get(rankUnderId);
        rankUnderProject(project, rankUnder);
        return project;
    }
    
    /** {@inheritDoc} */
    @Transactional
    public Project rankUnderProject(final Project project, Project upperProject)
            throws IllegalArgumentException {
        if (project == null) {
            throw new IllegalArgumentException("Project should be given");
        }
        
        rankingBusiness.rankUnder(project, upperProject, new RankUnderDelegate() {
            public Collection<? extends Rankable> getWithRankBetween(Integer lower, Integer upper) {
                return projectDAO.getProjectsWithRankBetween(lower, upper);
            }
        });

        return project;
    }
    
    /** {@inheritDoc} */
    @Transactional
    public void moveToRanked(int projectId) {
        LocalDate startDate = new LocalDate();
        LocalDate endDate = startDate.plus(settingBusiness
                .getPortfolioTimeSpan());
        List<Project> projects = projectDAO.getRankedProjects(startDate, endDate);
        Project project = projectDAO.get(projectId);
        
        if( projects.isEmpty()) {
            Project maxRankedProject = projectDAO.getMaxRankedProject();
            if(maxRankedProject == null) {
                project.setRank(1);
            } else {
                rankUnderProject(project,maxRankedProject);
            }   
            
        } else {
            rankUnderProject(project,projects.get(projects.size() - 1));
        }
        
    }

}
