

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.StdOut;

public class BaseballElimination {

	private int numberOfTeams;

	private Map<String, TeamInfo> teamsInfo;

	public BaseballElimination(String filename) {
		In in = new In(filename);
		this.numberOfTeams = in.readInt();
		teamsInfo = new LinkedHashMap<>();
		for (int index = 0; index < numberOfTeams; index++) {
			TeamInfo teamInfo = new TeamInfo(in.readString(), in.readInt(), in.readInt(), in.readInt(), index);
			int[] matchSchedule = new int[numberOfTeams];
			for (int sheduleIndex = 0; sheduleIndex < numberOfTeams; sheduleIndex++) {
				matchSchedule[sheduleIndex] = in.readInt();
			}
			teamInfo.withTeams = matchSchedule;
			teamsInfo.put(teamInfo.team, teamInfo);
		}
	} // create a baseball division from given filename in format specified below

	public int numberOfTeams() {
		return numberOfTeams;
	} // number of teams

	public Iterable<String> teams() {
		return teamsInfo.keySet();
	} // all teams

	public int wins(String team) {
		if(!teamsInfo.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		return teamsInfo.get(team).win;
	} // number of wins for given team

	public int losses(String team) {
		if(!teamsInfo.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		return teamsInfo.get(team).loss;
	} // number of losses for given team

	public int remaining(String team) {
		if(!teamsInfo.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		return teamsInfo.get(team).remaining;
	} // number of remaining games for given team

	public int against(String team1, String team2) {
		if(!teamsInfo.containsKey(team1) || !teamsInfo.containsKey(team2)) {
			throw new IllegalArgumentException();
		}
		return teamsInfo.get(team1).withTeams[teamsInfo.get(team2).index];
	} // number of remaining games between team1 and team2

	public boolean isEliminated(String team) {
		if(!teamsInfo.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		List<String> ans = trivialElimination(team);
		if (!ans.isEmpty()) {
			return true;
		}
		List<String> list = computeFlowNetwork(team);
		return list != null && !list.isEmpty();
	} // is given team eliminated?

	private List<String> trivialElimination(String team) {
		TeamInfo candidateTeam = teamsInfo.get(team);
		int maxWinForCandidateTeam = candidateTeam.win + candidateTeam.remaining;
		List<String> ans = new ArrayList<>();
		for (Map.Entry<String, TeamInfo> entry : teamsInfo.entrySet()) {
			if (entry.getValue().win > maxWinForCandidateTeam) {
				ans.add(entry.getValue().team);
			}
		}
		return ans;
	}

	private List<String> computeFlowNetwork(String team) {
		List<String> trivialEliminatedList = trivialElimination(team);
		if (!trivialEliminatedList.isEmpty()) {
			return trivialEliminatedList;
		}
		List<String> remaingTeam = new ArrayList<>();
		for (String existingTeam : teams()) {
			if (!existingTeam.equals(team)) {
				remaingTeam.add(existingTeam);
			}
		}
		int remainingTeamSize = numberOfTeams - 1;
		int totalPairVertex = (remainingTeamSize * (remainingTeamSize - 1)) / 2;
		int total = remainingTeamSize + totalPairVertex + 2;

		int startTeamVertex = 1 + totalPairVertex;

		FlowNetwork network = new FlowNetwork(total);
		int index = 1;
		for (int outer = 0; outer < remainingTeamSize - 1; outer++) {
			for (int inner = outer + 1; inner < remainingTeamSize; inner++) {
				network.addEdge(new FlowEdge(0, index, against(remaingTeam.get(outer), remaingTeam.get(inner))));
				network.addEdge(new FlowEdge(index, startTeamVertex + outer, Double.POSITIVE_INFINITY));
				network.addEdge(new FlowEdge(index, startTeamVertex + inner, Double.POSITIVE_INFINITY));
				index++;
			}
		}
		for (int teamVertex = startTeamVertex; teamVertex < startTeamVertex + remainingTeamSize; teamVertex++) {
			double capacity = teamsInfo.get(team).win + teamsInfo.get(team).remaining
					- teamsInfo.get(remaingTeam.get(teamVertex - startTeamVertex)).win;
			if (capacity >= 0.0) {
				network.addEdge(new FlowEdge(teamVertex, total - 1, capacity));
			}
		}
		List<String> list = new ArrayList<>();
		FordFulkerson fordFulkerson = new FordFulkerson(network, 0, total - 1);
		for (int start = startTeamVertex; start < startTeamVertex + remainingTeamSize; start++) {
			if (fordFulkerson.inCut(start)) {
				list.add(remaingTeam.get(start - startTeamVertex));
			}
		}
		if (!verifyCerticateOfElimination(list, team)) {
			return null;
		}
		return list;
	}

	private boolean verifyCerticateOfElimination(List<String> list, String team) {
		int remaining = 0;

		for (int outer = 0; outer < list.size() - 1; outer++) {
			for (int inner = outer + 1; inner < list.size(); inner++) {
				remaining += against(list.get(outer), list.get(inner));
			}
		}
		int win = 0;
		for (String winTeam : list) {
			win += teamsInfo.get(winTeam).win;
		}

		int total = win + remaining;
		double score = total / Double.valueOf(list.size());
		return teamsInfo.get(team).win + teamsInfo.get(team).remaining < Math.ceil(score);
	}

	public Iterable<String> certificateOfElimination(String team) {
		if(!teamsInfo.containsKey(team)) {
			throw new IllegalArgumentException();
		}
		return computeFlowNetwork(team);
	} // subset R of teams that eliminates given team; null if not eliminated

	public static void main(String[] args) {
		BaseballElimination division = new BaseballElimination("baseball/teams54.txt");
		for (String team : division.teams()) {
			if (division.isEliminated(team)) {
				StdOut.print(team + " is eliminated by the subset R = { ");
				for (String t : division.certificateOfElimination(team)) {
					StdOut.print(t + " ");
				}
				StdOut.println("}");
			} else {
				StdOut.println(team + " is not eliminated");
			}
		}
	}

	private static class TeamInfo {
		private String team;
		private int win;
		private int loss;
		private int remaining;
		private int[] withTeams;
		private int index;

		public TeamInfo(String team, int win, int loss, int remaining, int index) {
			super();
			this.team = team;
			this.win = win;
			this.loss = loss;
			this.remaining = remaining;
			this.index = index;
		}
	}
}
