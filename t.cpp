enum eOpCodeId {
    LOAD = 0,
    STORE
    MOV,
    ADD.
    JMP,
    CMP_LT
};

struct sOpCodeBase {

    public:
        sOpCodeBase( eOpCodeId _e, char const * _name, unsigned int _numOfArgs ) :
            mOpCodeId( _e ), 
            mName( _name ),
            mNumOfArgs( _numOfArgs )
    { }

        void exec( sEnv & _env ) {
            uint8_t * args;
        }

        eOpCodeId getOpCodeId() const {return mOpCodeId;}

        std::string const & getName(void) const {return mName;}


    protected:
        virtual void setArgs( uint8_t const * _src ) = 0;

        eOpCodeId mOpCodeId;
        std::string mName;
        unsigned int mNumOfArgs;
};


template<eOpCode e, unsigned int _args>
struct sOpCode : public sOpCodeBase {

    public:
        sOpCode() : sOpCodeBase( e, "pooo", _args) { }

        final void exec( sEnv & _env ) {
            uint8_t * src;
            memcpy( mArgBuffer.data(), src, mNumOfArgs);
            doInstruction (sEnv & _env);
        }

    protected:
        void doInstruction( sEnv & _env );
        std::array<uint8_t, _args> mArgBuffer;
};

