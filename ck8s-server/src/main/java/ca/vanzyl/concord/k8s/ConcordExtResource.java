package ca.vanzyl.concord.k8s;

import com.walmartlabs.concord.common.validation.ConcordKey;
import com.walmartlabs.concord.db.MainDB;
import com.walmartlabs.concord.server.OperationResult;
import com.walmartlabs.concord.server.audit.AuditAction;
import com.walmartlabs.concord.server.UuidGenerator;
import com.walmartlabs.concord.server.audit.AuditLog;
import com.walmartlabs.concord.server.audit.AuditObject;
import com.walmartlabs.concord.server.org.OrganizationEntry;
import com.walmartlabs.concord.server.org.OrganizationManager;
import com.walmartlabs.concord.server.org.team.*;
import com.walmartlabs.concord.server.sdk.rest.Resource;
import com.walmartlabs.concord.server.sdk.validation.Validate;
import com.walmartlabs.concord.server.security.UserPrincipal;
import org.jooq.Configuration;
import org.jooq.DSLContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.UUID;

import static com.walmartlabs.concord.server.jooq.tables.Teams.TEAMS;

@Path("/api/v1/ext")
public class ConcordExtResource implements Resource {

    private final OrganizationManager orgManager;
    private final TeamExtDao teamDao;
    private final TeamResource teamResource;
    private final TeamManager teamManager;
    private final AuditLog auditLog;

    @Inject
    public ConcordExtResource(OrganizationManager orgManager,
                              TeamExtDao teamDao,
                              TeamResource teamResource,
                              TeamManager teamManager,
                              AuditLog auditLog) {

        this.orgManager = orgManager;
        this.teamDao = teamDao;
        this.teamResource = teamResource;
        this.teamManager = teamManager;
        this.auditLog = auditLog;
    }

    @POST
    @Path("/{orgName}/team")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Validate
    public CreateTeamResponse createOrUpdateTeam(@PathParam("orgName") @ConcordKey String orgName,
                                                 @Valid TeamEntry entry) {

        if (entry.getId() == null) {
            return teamResource.createOrUpdate(orgName, entry);
        }

        TeamEntry team = teamDao.get(entry.getId());
        if (team != null) {
            return teamResource.createOrUpdate(orgName, entry);
        }

        OrganizationEntry org = orgManager.assertAccess(orgName, true);
        teamManager.assertAccess(org.getId(), TeamRole.OWNER);

        teamDao.tx(tx -> {
            UUID tId = teamDao.insert(tx, org.getId(), entry.getId(), entry.getName(), entry.getDescription());

            // add the current user as a team maintainer
            UUID userId = UserPrincipal.assertCurrent().getId();
            teamDao.upsertUser(tx, tId, userId, TeamRole.MAINTAINER);
        });

        auditLog.add(AuditObject.TEAM, AuditAction.CREATE)
                .field("orgId", org.getId())
                .field("teamId", entry.getId())
                .field("name", entry.getName())
                .changes(null, new TeamEntry(entry.getId(), org.getId(), null, entry.getName(), entry.getDescription()))
                .log();

        return new CreateTeamResponse(OperationResult.CREATED, entry.getId());
    }

    @Named
    public static class TeamExtDao extends TeamDao {

        @Inject
        public TeamExtDao(@MainDB Configuration cfg, UuidGenerator uuidGenerator) {
            super(cfg, uuidGenerator);
        }

        @Override
        public <T> T txResult(TxResult<T> t) {
            return super.txResult(t);
        }

        public UUID insert(DSLContext tx, UUID orgId, UUID teamId, String name, String description) {
            return tx.insertInto(TEAMS)
                    .columns(TEAMS.ORG_ID, TEAMS.TEAM_ID, TEAMS.TEAM_NAME, TEAMS.DESCRIPTION)
                    .values(orgId, teamId, name, description)
                    .returning(TEAMS.TEAM_ID)
                    .fetchOne()
                    .getTeamId();
        }
    }
}
