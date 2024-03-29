const { ApolloServer } = require('apollo-server');
const { ApolloGateway } = require('@apollo/gateway');

const gateway = new ApolloGateway({
    serviceList: [
        { name: 'film', url: 'http://localhost:8080/film/graphql' },
        { name: 'review', url: 'http://localhost:8080/review/graphql' },
        { name: 'artist', url: 'http://localhost:8080/artist/graphql' },
    ],
});

const server = new ApolloServer({
    gateway,
    subscriptions: false,
});

server.listen().then(({ url }) => {
    console.log(`🚀 Gateway ready at ${url}`);
});
