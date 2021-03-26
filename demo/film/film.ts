import { ApolloServer, gql } from "apollo-server";
import { buildFederatedSchema } from "@apollo/federation";

const films = [
    { id: "1", title: "Batman", year: "1989" },
    { id: "2", title: "Batman Returns", year: "1992" },
    { id: "3", title: "Batman: The Animated Series", year: "1992" },
];

const typeDefs = gql`
    type Query {
        films: [Film]
    }
    type Film @key(fields: "id") {
        id: ID!
        title: String
        year: String
    }
`;

const resolvers = {
    Query: {
        films() {
            return films;
        },
    },
    Film: {
        __resolveReference(film) {
            return films.find(c => c.id === film.id);
        },
    },
};

const server = new ApolloServer({
    schema: buildFederatedSchema([{ typeDefs, resolvers }]),
});

server.listen(4001).then(({ url }) => {
    console.log(`🚀 Film service ready at ${url}`);
});
